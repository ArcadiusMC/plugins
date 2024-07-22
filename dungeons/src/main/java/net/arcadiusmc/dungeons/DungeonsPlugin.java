package net.arcadiusmc.dungeons;

import lombok.Getter;
import net.arcadiusmc.dungeons.commands.CommandDungeons;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class DungeonsPlugin extends JavaPlugin {

  DungeonManager manager;
  SessionMap sessions;

  public static DungeonsPlugin plugin() {
    return JavaPlugin.getPlugin(DungeonsPlugin.class);
  }

  public static DungeonManager getManager() {
    return plugin().manager;
  }

  public static SessionMap getSessions() {
    return plugin().sessions;
  }

  @Override
  public void onEnable() {
    manager = new DungeonManager(this);
    sessions = new SessionMap();

    reloadConfig();

    new CommandDungeons();
  }

  @Override
  public void onDisable() {
    if (manager != null) {
      manager.shutdown();
    }
  }

  @Override
  public void reloadConfig() {
    manager.reload();
  }
}
