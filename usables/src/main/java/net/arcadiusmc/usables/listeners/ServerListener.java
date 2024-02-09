package net.arcadiusmc.usables.listeners;

import net.arcadiusmc.events.EarlyShutdownEvent;
import net.arcadiusmc.usables.UsablesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

class ServerListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onServerLoad(ServerLoadEvent event) {
    UsablesPlugin plugin = UsablesPlugin.get();
    plugin.freezeRegistries();
    plugin.reload();
  }

  @EventHandler(ignoreCancelled = true)
  public void onEarlyShutdown(EarlyShutdownEvent event) {
    UsablesPlugin plugin = UsablesPlugin.get();
    plugin.save();
  }
}
