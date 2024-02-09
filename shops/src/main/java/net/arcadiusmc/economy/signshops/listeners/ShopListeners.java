package net.arcadiusmc.economy.signshops.listeners;

import net.arcadiusmc.economy.signshops.ShopManager;
import net.arcadiusmc.events.Events;

public class ShopListeners {

  public static void registerAll(ShopManager manager) {
    Events.register(new ShopCreateListener(manager));
    Events.register(new ShopDestroyListener(manager));
    Events.register(new ShopInteractionListener(manager));
    Events.register(new ShopInventoryListener());
  }
}
