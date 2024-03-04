package net.arcadiusmc.waypoints.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Cooldown;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointConfig;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.visit.WaypointVisit;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandSpawn extends BaseCommand {

  public CommandSpawn() {
    super("spawn");
    setPermission(WPermissions.WAYPOINTS);
    setDescription("Alias for '/vr Hazelguard'");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      Cooldown.testAndThrow(c.getSource(), "waypoint_visit", 5 * 20);

      User user = getUserSender(c);

      WaypointManager manager = WaypointManager.getInstance();
      WaypointConfig config = manager.config();
      String spawnWaypoint = config.spawnWaypoint;

      Waypoint waypoint = manager.getExtensive(spawnWaypoint);

      if (waypoint == null) {
        throw Exceptions.create("No spawn region exists! Tell the admins");
      }

      WaypointVisit.visit(user, waypoint);
      return 0;
    });
  }
}
