package net.arcadiusmc.waypoints.listeners;

import net.arcadiusmc.waypoints.AutoGen;
import net.arcadiusmc.waypoints.WaypointManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class AutoGenListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onChunkLoad(ChunkLoadEvent event) {
    WaypointManager manager = WaypointManager.getInstance();
    Chunk chunk = event.getChunk();

    AutoGen.maybePlace(chunk, manager);
  }
}
