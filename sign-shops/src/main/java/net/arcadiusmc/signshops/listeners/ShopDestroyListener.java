package net.arcadiusmc.signshops.listeners;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import net.arcadiusmc.signshops.SMessages;
import net.arcadiusmc.signshops.SPermissions;
import net.arcadiusmc.signshops.ShopManager;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShops;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ShopDestroyListener implements Listener {

  private final ShopManager manager;

  public ShopDestroyListener(ShopManager manager) {
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onSignDestroy(BlockBreakEvent event) {
    if (!SignShops.isShop(event.getBlock())) {
      return;
    }

    SignShop shop = manager.getShop(event.getBlock());
    Player player = event.getPlayer();

    if (!player.getUniqueId().equals(shop.getOwner())
        && !player.hasPermission(SPermissions.ADMIN)
    ) {
      player.sendMessage(SMessages.cannotDestroy(player));
      return;
    }

    shop.destroy(false);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockDestroy(BlockDestroyEvent event) {
    //Don't allow the block to be broken if it's a shop
    if (SignShops.isShop(event.getBlock())) {
      event.setCancelled(true);
    }
  }

}