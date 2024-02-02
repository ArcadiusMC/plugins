package net.arcadiusmc.core.listeners;

import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class HopperListener implements Listener {

  private final CorePlugin plugin;

  public HopperListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    int max = plugin.getCoreConfig().hoppersInOneChunk();

    if (event.getBlock().getType() != Material.HOPPER || max == -1) {
      return;
    }

    int hopperAmount = event.getBlock()
        .getChunk()
        .getTileEntities(block -> block.getType() == Material.HOPPER, true)
        .size();

    if (hopperAmount <= max) {
      return;
    }

    event.setCancelled(true);
    event.getPlayer().sendMessage(
        CoreMessages.HOPPER_WARNING.renderText(event.getPlayer())
    );
  }
}
