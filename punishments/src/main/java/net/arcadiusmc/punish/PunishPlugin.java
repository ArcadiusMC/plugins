package net.arcadiusmc.punish;

import java.nio.file.Path;
import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.punish.commands.PunishCommands;
import net.arcadiusmc.punish.listeners.PunishListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PunishPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private PunishConfig pluginConfig;

  private PunishManager punishManager;
  private JailManager jails;
  private BannedWords bannedWords;

  private PeriodicalSaver saver;

  public static PunishPlugin plugin() {
    return getPlugin(PunishPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    Path dataDir = getDataFolder().toPath();

    bannedWords = new BannedWords(dataDir);
    punishManager = new PunishManager(dataDir.resolve("userdata.dat"));
    jails = new JailManager(dataDir.resolve("jails.dat"));

    reloadConfig();

    jails.load();
    punishManager.load();

    saver = PeriodicalSaver.create(this::save, () -> Duration.ofMinutes(30));
    saver.start();

    PunishListeners.registerAll(this);
    PunishCommands.registerAll(this);

    SettingsBook<User> settingsBook = ArcadiusServer.server().getGlobalSettingsBook();
    PunishPrefs.createSettings(settingsBook);
    EavesDropper.createSettings(settingsBook);
  }

  @Override
  public void onDisable() {
    save();
    saver.stop();
    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    pluginConfig = TomlConfigs.loadPluginConfig(this, PunishConfig.class);

    if (bannedWords != null) {
      bannedWords.load();
    }
  }

  public void save() {
    // Null checks in case error was thrown during initialization
    // and that prevented instantiation

    if (punishManager != null) {
      punishManager.save();
    }
    if (jails != null) {
      jails.save();
    }
  }
}
