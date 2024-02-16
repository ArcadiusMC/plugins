package net.arcadiusmc.punish.listeners;

import net.arcadiusmc.utils.PluginUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.slf4j.Logger;

class ServerLoadListener implements Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onServerLoad(ServerLoadEvent event) {
    Logger logger = PluginUtil.getPlugin().getSLF4JLogger();
  }
}
