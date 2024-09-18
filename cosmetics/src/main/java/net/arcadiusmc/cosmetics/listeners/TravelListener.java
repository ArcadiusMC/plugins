package net.arcadiusmc.cosmetics.listeners;

import net.arcadiusmc.cosmetics.ActiveMap;
import net.arcadiusmc.cosmetics.Cosmetic;
import net.arcadiusmc.cosmetics.CosmeticsPlugin;
import net.arcadiusmc.cosmetics.travel.TravelEffect;
import net.arcadiusmc.cosmetics.travel.TravelEffects;
import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.event.WaypointVisitEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TravelListener implements Listener {

  private final CosmeticsPlugin plugin;

  public TravelListener(CosmeticsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onWaypointVisit(WaypointVisitEvent event) {
    ActiveMap map = plugin.getActiveMap();
    User user = event.getUser();

    Cosmetic<TravelEffect> cosmetic = map.getActive(user.getUniqueId(), TravelEffects.type);
    if (cosmetic == null) {
      return;
    }

    TravelEffect effect = cosmetic.getValue();
    Location location = event.getCurrentLocation();
    Location destination = event.getDestination();

    switch (event.getType()) {
      case INSTANT_TELEPORT -> {
        effect.onPoleTeleport(user, location, destination);
      }
      case LAND -> {
        effect.onHulkLand(user, location);
      }
      case HULK_BEGIN -> {
        effect.onHulkStart(user, location);
      }
      case TICK_DOWN -> {
        effect.onHulkTickDown(user, location);
      }
      case TICK_UP -> {
        effect.onHulkTickUp(user, location);
      }
    }
  }
}
