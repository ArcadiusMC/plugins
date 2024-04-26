package net.arcadiusmc.merchants.listeners;

import net.arcadiusmc.events.Events;
import net.arcadiusmc.merchants.MerchantsPlugin;

public class MerchantListeners {

  public static void registerAll(MerchantsPlugin plugin) {
    Events.register(new DayChangeListener(plugin));
    Events.register(new ParrotListener(plugin));
  }
}
