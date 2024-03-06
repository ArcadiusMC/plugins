package net.arcadiusmc.waypoints.visit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.Waypoints;
import net.forthecrown.grenadier.SyntaxExceptions;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.WaypointProperties;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.spongepowered.math.vector.Vector3i;

public interface VisitPredicate {

  VisitPredicate RIDING_VEHICLE = visit -> {
    Entity entity = visit.getUser().getPlayer().getVehicle();
    if (entity == null || entity instanceof Vehicle) {
      return;
    }

    throw WExceptions.onlyInVehicle();
  };

  VisitPredicate NOT_AT_SAME = visit -> {
    if (!visit.isNearWaypoint()) {
      return;
    }
    var near = visit.getNearestWaypoint();

    if (!near.equals(visit.getDestination())) {
      return;
    }

    throw Exceptions.create("Already at destination waypoint");
  };

  VisitPredicate IS_NEAR = visit -> {
    User player = visit.getUser();

    if (player.hasPermission(WPermissions.WAYPOINTS_ADMIN)) {
      return;
    }

    if (visit.isNearWaypoint()) {
      return;
    }

    Vector3i nearestPoint = Waypoints.findNearestProbable(player);

    if (nearestPoint == null) {
      throw WExceptions.farFromWaypoint();
    } else {
      throw WExceptions.farFromWaypoint(nearestPoint);
    }
  };

  VisitPredicate IS_DISCOVERED = visit -> {
    User player = visit.getUser();
    Waypoint waypoint = visit.getDestination();

    if (player.hasPermission(WPermissions.IGNORE_DISCOVERY)
        || !waypoint.get(WaypointProperties.REQUIRES_DISCOVERY)
        || waypoint.hasDiscovered(player.getUniqueId())
    ) {
      return;
    }

    throw WExceptions.waypointNotDiscovered(waypoint);
  };

  VisitPredicate DESTINATION_VALID = waypointIsValid(true);
  VisitPredicate NEAREST_VALID = waypointIsValid(false);

  /**
   * Tests if the visit is allowed to continue
   * <p></p>
   * Predicates are the first thing called when a region visit is ran
   *
   * @param visit The visit to check
   * @throws CommandSyntaxException If the check failed
   */
  void test(WaypointVisit visit) throws CommandSyntaxException;

  private static VisitPredicate waypointIsValid(boolean dest) {
    return visit -> {
      if (visit.getUser().hasPermission(WPermissions.WAYPOINTS_ADMIN)) {
        return;
      }

      if (dest && !visit.getDestination().isWorldLoaded()) {
        throw WExceptions.unloadedWorld();
      }

      var waypoint = dest
          ? visit.getDestination()
          : visit.getNearestWaypoint();

      if (!dest && !visit.isNearWaypoint()) {
        return;
      }

      // Should only happen if the nearest
      // waypoint is null, in the case of admins TPing
      // from worlds with no waypoints, which should be
      // checked by a preceding predicate
      if (waypoint == null || waypoint.get(WaypointProperties.INVULNERABLE)) {
        return;
      }

      Optional<CommandSyntaxException> exc = waypoint.getType().isValid(waypoint)
          .map(e -> {
            Component msg = SyntaxExceptions.formatCommandException(e);

            // Prefix with either 'target' or 'nearest' to
            // make the message a bit more readable
            if (dest) {
              msg = Component.text("Target ").append(msg);
            } else {
              msg = Component.text("Nearest ").append(msg);
            }

            return Exceptions.create(msg);
          });

      if (exc.isEmpty()) {
        return;
      }

      throw exc.get();
    };
  }
}