package net.arcadiusmc.challenges.listeners;

import net.arcadiusmc.challenges.ChallengeManager;
import net.arcadiusmc.challenges.Challenges;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.sellshop.event.SellShopCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class SellShopListener implements Listener {

  private final ChallengeManager manager;

  public SellShopListener(ChallengeManager manager) {
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true)
  public void onSellShopCreate(SellShopCreateEvent event) {
    var builder = event.getBuilder();

    builder.add(
        Slot.of(4, 2),
        Menus.createOpenNode(
            manager::getItemChallengeMenu,
            Challenges.createMenuHeader()
        )
    );
  }
}
