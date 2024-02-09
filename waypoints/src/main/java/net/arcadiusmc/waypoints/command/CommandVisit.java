package net.arcadiusmc.waypoints.command;

import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.ParseResult;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.utils.Cooldown;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.visit.WaypointVisit;

public class CommandVisit extends FtcCommand {

  public CommandVisit() {
    super("Visit");

    setAliases("v", "vr", "visitregion");
    setDescription("Visits a teleport waypoint");
    setPermission(WPermissions.WAYPOINTS);

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /Visit
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<waypoint name>", "Visits a teleport <waypoint>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("waypoint", WaypointCommands.WAYPOINT)
            .executes(c -> {
              Cooldown.testAndThrow(c.getSource(), "waypoint_visit", 5 * 20);

              ParseResult<Waypoint> result = c.getArgument("waypoint", ParseResult.class);
              Waypoint waypoint = result.get(c.getSource(), true);

              WaypointVisit.visit(getUserSender(c), waypoint);

              return 0;
            })
        );
  }
}