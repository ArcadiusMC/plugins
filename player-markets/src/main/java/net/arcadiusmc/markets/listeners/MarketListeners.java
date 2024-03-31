package net.arcadiusmc.markets.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.markets.MarketsPlugin;

public class MarketListeners {

  public static void registerAll(MarketsPlugin plugin) {
    register(new ServerLoadListener(plugin));
    register(new SignShopListener(plugin));
    register(new MarketClaimingListener(plugin));
  }
}
