package net.arcadiusmc.holograms.listeners;

import net.arcadiusmc.events.EarlyShutdownEvent;
import net.arcadiusmc.holograms.HologramPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerListener implements Listener {

  private final HologramPlugin plugin;

  public ServerListener(HologramPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onServerLoad(ServerLoadEvent event) {
    plugin.reload();
  }

  @EventHandler(ignoreCancelled = true)
  public void onEarlyShutdown(EarlyShutdownEvent event) {
    plugin.getService().save();
    plugin.getService().clear();
  }
}
