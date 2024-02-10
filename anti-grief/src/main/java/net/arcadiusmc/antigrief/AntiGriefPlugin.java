package net.arcadiusmc.antigrief;

import lombok.Getter;
import net.arcadiusmc.FtcServer;
import net.arcadiusmc.antigrief.commands.AntiGriefCommands;
import net.arcadiusmc.antigrief.listeners.AntiGriefListeners;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class AntiGriefPlugin extends JavaPlugin {

  private AntiGriefConfig pluginConfig = new AntiGriefConfig();
  private PeriodicalSaver saver;

  @Override
  public void onEnable() {
    reloadConfig();

    saver = PeriodicalSaver.create(this::save, () -> pluginConfig.getAutosaveInterval());
    saver.start();

    Punishments.get().load();

    AntiGriefCommands.createCommands();
    AntiGriefListeners.registerAll(this);

    FtcServer server = FtcServer.server();
    SettingsBook<User> settingsBook = server.getGlobalSettingsBook();

    EavesDropper.createSettings(settingsBook);
    StaffChat.createSettings(settingsBook);
    StaffNote.createSettings(settingsBook);
  }

  @Override
  public void reloadConfig() {
    pluginConfig = TomlConfigs.loadPluginConfig(this, AntiGriefConfig.class);
    BannedWords.load();
  }

  public void reload() {
    reloadConfig();
    Punishments.get().load();
  }

  @Override
  public void onDisable() {
    save();
    saver.stop();
  }

  void save() {
    Punishments.get().save();
  }
}