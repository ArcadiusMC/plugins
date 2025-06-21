package net.arcadiusmc.dungeons;

import lombok.Getter;
import net.arcadiusmc.dungeons.commands.CommandDungeon;
import net.arcadiusmc.dungeons.commands.CommandDungeonGen;
import net.arcadiusmc.text.loader.MessageLoader;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class DungeonsPlugin extends JavaPlugin {

  private SessionManager manager;
  private LevelTypeManager levelTypes;
  private SettingsManager settings;

  public static DungeonsPlugin plugin() {
    return getPlugin(DungeonsPlugin.class);
  }

  @Override
  public void onEnable() {
    levelTypes = new LevelTypeManager(this);
    settings = new SettingsManager(this);

    manager = new SessionManager(this);
    manager.startTicking();

    new CommandDungeonGen();
    new CommandDungeon(this);

    reload();
  }

  @Override
  public void onDisable() {
    if (manager != null) {
      manager.close();
    }
    if (settings != null) {
      settings.save();
    }
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);
    levelTypes.load();
  }

  public void reload() {
    reloadConfig();

    if (settings != null) {
      settings.load();
    }
  }
}
