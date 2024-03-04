package net.arcadiusmc.waypoints.command;

import static net.arcadiusmc.waypoints.WaypointPrefs.INVITES_ALLOWED;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WMessages;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointHomes;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Sound;

public class CommandInvite extends BaseCommand {

  public CommandInvite() {
    super("Invite");

    setDescription("Invites users to your home waypoint");
    setPermission(WPermissions.WAYPOINTS);

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<users>", getDescription());
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("users", Arguments.ONLINE_USERS)
            .executes(c -> {
              var user = getUserSender(c);

              if (!user.get(INVITES_ALLOWED)) {
                throw Exceptions.format("You have inviting turned off");
              }

              Optional<Waypoint> waypoint = WaypointHomes.getHome(user);

              if (waypoint.isEmpty()) {
                throw WExceptions.noHomeRegion();
              }

              List<User> targets = Arguments.getUsers(c, "users");

              Optional<CommandSyntaxException> opt = UserBlockList.filterPlayers(
                  user,
                  targets,
                  INVITES_ALLOWED,
                  Messages.reference("waypoints.invites.blocked"),
                  Messages.reference("waypoints.invites.self")
              ).map(Exceptions::create);

              if (opt.isPresent()) {
                throw opt.get();
              }

              for (User target : targets) {
                waypoint.get().invite(user.getUniqueId(), target.getUniqueId());

                target.sendMessage(WMessages.targetInvited(user));
                target.playSound(Sound.UI_TOAST_IN, 2, 1.3f);
                user.sendMessage(WMessages.senderInvited(target));
              }

              if (targets.size() > 1) {
                user.sendMessage(WMessages.invitedTotal(targets.size()));
              }

              user.playSound(Sound.UI_TOAST_OUT, 2, 1.5f);

              return 0;
            })
        );
  }
}