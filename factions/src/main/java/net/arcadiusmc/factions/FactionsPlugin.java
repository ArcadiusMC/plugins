package net.arcadiusmc.factions;

import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.factions.commands.FactionCommands;
import net.arcadiusmc.factions.listeners.FactionListeners;
import net.arcadiusmc.factions.usables.FactionUsables;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionsPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Getter
  private final FactionsConfig pluginConfig = new FactionsConfig();

  private PeriodicalSaver saver;

  @Getter
  private FactionManager manager;

  public static FactionsPlugin plugin() {
    return getPlugin(FactionsPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    Properties.registerAll();
    manager = new FactionManager(this);

    reloadConfig();
    manager.load();

    FactionCommands.createCommands(this);
    FactionUsables.registerAll();
    FactionListeners.registerAll(this);

    saver = PeriodicalSaver.create(this::save, Duration.ofMinutes(30));
    saver.start();

    UserNameFactory factory = Users.getService().getNameFactory();
    factory.addProfileField(FactionProfileElement.ID, 35, new FactionProfileElement());
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild(getName());

    UserNameFactory factory = Users.getService().getNameFactory();
    factory.removeField(FactionProfileElement.ID);

    if (manager != null) {
      manager.save();
    }

    if (saver != null) {
      saver.stop();
    }
  }

  public void save() {
    manager.save();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    PluginJar.saveResources(this, "config.yml");

    Path path = getDataFolder().toPath().resolve("config.yml");

    SerializationHelper.readAsJson(path, object -> {
      FactionsConfig.CODEC.decode(JsonOps.INSTANCE, object, pluginConfig)
          .mapError(s -> "Failed to load Factions config: " + s)
          .resultOrPartial(getSLF4JLogger()::error);

      int min = pluginConfig.getMinReputation();
      int max = pluginConfig.getMaxReputation();

      if (min >= max) {
        getSLF4JLogger().error("min-reputation is larger than max-reputation");
        pluginConfig.minMaxReputation();
      }
    });
  }
}
