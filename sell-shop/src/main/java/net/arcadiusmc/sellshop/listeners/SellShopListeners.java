package net.arcadiusmc.sellshop.listeners;

import net.arcadiusmc.events.Events;
import net.arcadiusmc.sellshop.SellShopPlugin;

public class SellShopListeners {

  public static void registerAll(SellShopPlugin plugin) {
    Events.register(new AutoSellListener(plugin));
    Events.register(new PlayerJoinListener());
  }
}
