package net.arcadiusmc.waypoints.listeners;

import java.util.Optional;
import net.arcadiusmc.user.event.HomeCommandEvent;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointHomes;
import net.arcadiusmc.waypoints.Waypoints;
import net.arcadiusmc.waypoints.visit.WaypointVisit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;

public class HomeListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onHomeCommand(HomeCommandEvent event) {
    if (event.isNameSet()) {
      return;
    }

    var user = event.getUser();
    BoundingBox playerBounds = user.getPlayer().getBoundingBox();
    Waypoint waypoint = Waypoints.getColliding(user.getPlayer());

    if (waypoint == null || !waypoint.getBounds().overlaps(playerBounds)) {
      return;
    }

    Optional<Waypoint> home = WaypointHomes.getHome(user);

    if (home.isEmpty()) {
      return;
    }

    event.setCancelled(true);
    WaypointVisit.visit(user, home.get());
  }
}
