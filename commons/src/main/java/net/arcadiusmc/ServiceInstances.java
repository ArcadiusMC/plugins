package net.arcadiusmc;

import static net.arcadiusmc.BukkitServices.loadOrThrow;

final class ServiceInstances {

  static Cooldowns cooldown;
  static InventoryStorage inventoryStorage;
  static ItemGraveService grave;
  static ArcadiusServer server;

  public static Cooldowns getCooldown() {
    return cooldown == null
        ? (cooldown = loadOrThrow(Cooldowns.class))
        : cooldown;
  }

  public static InventoryStorage getInventoryStorage() {
    return inventoryStorage == null
        ? (inventoryStorage = loadOrThrow(InventoryStorage.class))
        : inventoryStorage;
  }

  public static ItemGraveService getGrave() {
    return grave == null
        ? (grave = loadOrThrow(ItemGraveService.class))
        : grave;
  }

  public static ArcadiusServer getServer() {
    return server == null
        ? (server = loadOrThrow(ArcadiusServer.class))
        : server;
  }
}