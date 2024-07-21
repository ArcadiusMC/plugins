package net.arcadiusmc.ui;

import lombok.Getter;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.ui.commands.CommandEntityUi;
import net.arcadiusmc.ui.listeners.PageListeners;
import net.arcadiusmc.ui.resource.PageLoader;
import net.arcadiusmc.utils.io.PluginJar;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class UiPlugin extends JavaPlugin {

  private Sessions sessions;
  private PageLoader loader;

  @Override
  public void onEnable() {
    sessions = new Sessions();
    sessions.startTask();

    loader = new PageLoader(this);
    loader.loadDefaultStyles();

    PluginJar.saveResources("menus/test-page");

    Events.register(new PageListeners(this));
    new CommandEntityUi(this);
  }

  @Override
  public void onDisable() {
    sessions.stopTask();
    sessions.kill();
  }
}
