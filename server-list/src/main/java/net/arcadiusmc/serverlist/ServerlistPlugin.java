package net.arcadiusmc.serverlist;

import lombok.Getter;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ServerlistPlugin extends JavaPlugin {

  private ServerListDisplay display;
  private ServerListConfig listConfig;

  @Override
  public void onEnable() {
    display = new ServerListDisplay();
    reload();

    new CommandServerList();
    Events.register(new ServerlistListener(this));
  }

  public void reload() {
    reloadConfig();
    display.load();
  }

  @Override
  public void reloadConfig() {
    listConfig = TomlConfigs.loadPluginConfig(this, ServerListConfig.class);
  }
}
