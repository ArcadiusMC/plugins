package net.arcadiusmc.waypoints.command;

import java.util.Objects;
import java.util.Optional;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WMessages;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointHomes;
import net.arcadiusmc.waypoints.Waypoints;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Sound;

public class CommandMoveIn extends BaseCommand {

  public CommandMoveIn() {
    super("MoveIn");

    setPermission(WPermissions.WAYPOINTS);
    setDescription("Sets your home waypoint");
    setAliases("sethomepole", "sethomepost");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /MoveIn
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
          Waypoint waypoint = Waypoints.getNearest(user);

          if (waypoint == null) {
            throw WExceptions.farFromWaypoint();
          } else if (!waypoint.getBounds().contains(user.getPlayer())) {
            throw WExceptions.farFromWaypoint(waypoint);
          }

          Optional<Waypoint> currentOpt = WaypointHomes.getHome(user);
          if (currentOpt.isPresent()) {
            Waypoint currentHome = currentOpt.get();

            if (Objects.equals(waypoint, currentHome)) {
              throw Messages.render("waypoints.errors.alreadySet.here")
                  .exception(user);
            }
          }

          user.sendMessage(WMessages.HOME_WAYPOINT_SET);
          user.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);

          waypoint.addResident(user.getUniqueId());

          return 0;
        });
  }
}