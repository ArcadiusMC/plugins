package net.arcadiusmc.sellshop.listeners;

import net.arcadiusmc.events.Events;
import net.arcadiusmc.sellshop.SellShopPlugin;

public class SellShopListeners {

  public static void registerAll(SellShopPlugin plugin) {
    Events.register(new AutoSellListener());
    Events.register(new ServerLoadListener(plugin));
    Events.register(new PlayerJoinListener());
  }
}
