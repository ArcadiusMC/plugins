package net.arcadiusmc.markets;

import com.mojang.serialization.JsonOps;
import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.markets.autoevict.AutoEvictions;
import net.arcadiusmc.markets.command.MarketCommands;
import net.arcadiusmc.markets.gui.ShopLists;
import net.arcadiusmc.markets.listeners.MarketListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class MarketsPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private MarketsConfig pluginConfig = MarketsConfig.EMPTY;

  private PeriodicalSaver saver;

  private MarketsManager manager;
  private AutoEvictions autoEvictions;
  private MarketResets resets;
  private Debts debts;
  private ClaimHighlighter highlighter;
  private ShopLists lists;

  public static MarketsPlugin plugin() {
    return getPlugin(MarketsPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    saver = PeriodicalSaver.create(this::save, Duration.ofMinutes(30));
    saver.start();

    manager = new MarketsManager(this);
    autoEvictions = new AutoEvictions(this);
    resets = new MarketResets(this);
    debts = new Debts(this);
    highlighter = new ClaimHighlighter(this);
    lists = new ShopLists(this);

    reload();

    highlighter.schedule();

    MarketListeners.registerAll(this);
    MarketCommands.registerAll(this);
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild(getName());
    save();
  }

  @Override
  public void reloadConfig() {
    PluginJar.saveResources("config.yml");
    MessageLoader.loadPluginMessages(this, messageList);
    SerializationHelper.readAsJson(
        getDataFolder().toPath().resolve("config.yml"),
        jsonObject -> {
          MarketsConfig.CODEC.parse(JsonOps.INSTANCE, jsonObject)
              .mapError(s -> "Failed to load markets config: " + s)
              .resultOrPartial(getSLF4JLogger()::error)
              .ifPresent(marketsConfig -> this.pluginConfig = marketsConfig);
        }
    );

    resets.load();
    highlighter.load();
    lists.load();

    if (manager.isServerLoaded()) {
      manager.scheduleMarketTicker();
    }
  }

  public void reload() {
    reloadConfig();
    manager.load();
    autoEvictions.load();
    debts.load();
  }

  public void save() {
    if (manager != null) {
      manager.save();
    }

    if (autoEvictions != null) {
      autoEvictions.save();
    }

    if (debts != null) {
      debts.save();
    }
  }
}
