package net.arcadiusmc.items.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.items.ItemPlugin;

public final class ItemListeners {
  private ItemListeners() {}

  public static void registerAll(ItemPlugin plugin) {
    register(new AnvilListener(plugin));
    register(new WearableTagListener(plugin));
    register(new ItemCallbackListeners());
    register(new SmokeBomb());
    register(new NoCopiesListener());
    register(new GunListeners());
    register(new PiratesLuckListener());
    register(new DoubleDropListener(plugin));
    register(new SliceListener());
  }
}
