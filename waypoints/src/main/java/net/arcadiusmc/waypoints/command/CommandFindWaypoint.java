package net.arcadiusmc.waypoints.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoints;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.math.vector.Vector3i;

public class CommandFindWaypoint extends BaseCommand {

  public CommandFindWaypoint() {
    super("findwaypoint");

    setAliases("findpole", "findpost", "find-waypoint");
    setPermission(WPermissions.WAYPOINTS);
    setDescription("Shows you the nearest waypoint");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);

      Vector3i playerPos = Vectors.intFrom(user.getLocation());
      Vector3i waypointPos = Waypoints.findNearestProbable(user);

      if (waypointPos == null) {
        throw WExceptions.farFromWaypoint();
      }

      double distance = playerPos.distance(waypointPos);

      user.sendMessage(
          Text.format(
              "Nearest waypoint is at &e{0}&rx &e{1}&ry &e{2}&rz "
                  + "&r(&e{3, number, -floor}&r blocks away)",

              NamedTextColor.GRAY,

              waypointPos.x(), waypointPos.y(), waypointPos.z(),
              distance
          )
      );
      return 0;
    });
  }
}
