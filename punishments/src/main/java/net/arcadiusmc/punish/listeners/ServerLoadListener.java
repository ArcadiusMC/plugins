package net.arcadiusmc.punish.listeners;

import net.arcadiusmc.punish.PunishManager;
import net.arcadiusmc.punish.PunishPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

class ServerLoadListener implements Listener {

  private final PunishPlugin plugin;

  public ServerLoadListener(PunishPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onServerLoad(ServerLoadEvent event) {
    PunishManager manager = plugin.getPunishManager();
    manager.onServerStarted();
  }
}
