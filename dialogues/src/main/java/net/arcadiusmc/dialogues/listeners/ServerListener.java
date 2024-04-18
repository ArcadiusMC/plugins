package net.arcadiusmc.dialogues.listeners;

import net.arcadiusmc.dialogues.DialoguesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerListener implements Listener {

  private final DialoguesPlugin plugin;

  public ServerListener(DialoguesPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onServerLoad(ServerLoadEvent event) {
    plugin.getManager().load();
  }
}
