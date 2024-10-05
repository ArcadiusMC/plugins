package net.arcadiusmc.dungeons;

import net.arcadiusmc.dungeons.commands.CommandDungeonGen;
import net.arcadiusmc.text.loader.MessageLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonsPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    new CommandDungeonGen();
  }

  @Override
  public void onDisable() {

  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);
  }
}
