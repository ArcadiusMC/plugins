package net.arcadiusmc.waypoints.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.Waypoints;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.math.vector.Vector3i;

public class CommandFindPole extends BaseCommand {

  public CommandFindPole() {
    super("findpole");

    setAliases("findpost", "findwaypoint");
    setPermission(WPermissions.WAYPOINTS);
    setDescription("Shows you the nearest waypoint");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);
      Waypoint nearest = Waypoints.getNearest(user);

      if (nearest == null) {
        throw WExceptions.farFromWaypoint();
      }

      Vector3i playerPos = Vectors.intFrom(user.getLocation());
      Vector3i waypointPos = nearest.getPosition();

      double distance = playerPos.distance(waypointPos);

      user.sendMessage(
          Text.format(
              "Nearest waypoint is at &e{0}&6x &e{1}&6y &e{2}&6z "
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
