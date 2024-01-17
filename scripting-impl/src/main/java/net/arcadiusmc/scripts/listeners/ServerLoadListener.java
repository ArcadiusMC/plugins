package net.arcadiusmc.scripts.listeners;

import net.arcadiusmc.scripts.ScriptingPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

class ServerLoadListener implements Listener {

  private final ScriptingPlugin plugin;

  public ServerLoadListener(ScriptingPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onServerLoad(ServerLoadEvent event) {
    plugin.getPacks().activate();
  }
}
