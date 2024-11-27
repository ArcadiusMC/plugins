package net.arcadiusmc.dungeons;

import net.arcadiusmc.dungeons.commands.CommandDungeonGen;
import net.arcadiusmc.text.loader.MessageLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonsPlugin extends JavaPlugin {

  private SessionManager manager;

  @Override
  public void onEnable() {
    manager = new SessionManager();
    manager.startTicking();

    new CommandDungeonGen();
  }

  @Override
  public void onDisable() {
    if (manager != null) {
      manager.close();
    }
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);
  }
}
