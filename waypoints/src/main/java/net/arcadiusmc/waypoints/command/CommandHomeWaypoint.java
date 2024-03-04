package net.arcadiusmc.waypoints.command;

import java.util.Optional;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointHomes;
import net.arcadiusmc.waypoints.visit.WaypointVisit;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandHomeWaypoint extends BaseCommand {

  public CommandHomeWaypoint() {
    super("HomeWaypoint");

    setPermission(WPermissions.WAYPOINTS);
    setAliases("homepole", "homepost");
    setDescription("Takes you to your home waypoint");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /HomeWaypoint
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          Optional<Waypoint> home = WaypointHomes.getHome(user);

          if (home.isEmpty()) {
            throw WExceptions.noHomeRegion();
          }

          WaypointVisit.visit(user, home.get());
          return 0;
        });
  }
}