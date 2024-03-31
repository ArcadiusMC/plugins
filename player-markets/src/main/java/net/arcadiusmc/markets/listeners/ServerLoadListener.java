package net.arcadiusmc.markets.listeners;

import net.arcadiusmc.markets.MarketsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerLoadListener implements Listener {

  private final MarketsPlugin plugin;

  public ServerLoadListener(MarketsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onServerLoad(ServerLoadEvent event) {
    plugin.getManager().onServerLoaded();
    plugin.getAutoEvictions().schedule();
  }
}
