package net.arcadiusmc.core.commands.tpa;

import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandTpaCancel extends BaseCommand {

  public CommandTpaCancel() {
    super("tpacancel");

    setPermission(TpPermissions.TPA);
    setDescription("Cancels a tpa request");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              TeleportRequest r = TeleportRequests.getOutgoing(user, target);
              if (r == null) {
                throw Exceptions.noOutgoing(target);
              }

              r.cancel();
              return 0;
            })
        )

        .executes(c -> {
          User user = getUserSender(c);
          TeleportRequest r = TeleportRequests.latestOutgoing(user);

          if (r == null) {
            throw TpExceptions.NO_TP_REQUESTS_OUT;
          }

          r.cancel();
          return 0;
        });
  }
}