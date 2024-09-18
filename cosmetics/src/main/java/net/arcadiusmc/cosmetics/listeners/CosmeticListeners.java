package net.arcadiusmc.cosmetics.listeners;

import static net.arcadiusmc.events.Events.register;

import net.arcadiusmc.cosmetics.CosmeticsPlugin;
import net.arcadiusmc.utils.PluginUtil;

public class CosmeticListeners {

  public static void registerAll(CosmeticsPlugin plugin) {
    register(new DeathListener(plugin));
    register(new ArrowListener(plugin));
    register(new PlayerJoinListener());

    if (PluginUtil.isEnabled("Waypoints")) {
      register(new TravelListener(plugin));
    }
  }
}
