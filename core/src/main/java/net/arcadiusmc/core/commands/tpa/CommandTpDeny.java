package net.arcadiusmc.core.commands.tpa;

import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandTpDeny extends BaseCommand {

  public CommandTpDeny() {
    super("tpdeny");

    setPermission(TpPermissions.TPA);
    setDescription("Denies a tpa request");
    setAliases("tpadeny");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              User user = getUserSender(c);
              User sender = Arguments.getUser(c, "user");
              TeleportRequest request = TeleportRequests.getIncoming(user, sender);

              if (request == null) {
                throw Exceptions.noIncoming(sender);
              }

              request.deny();
              return 0;
            })
        )

        .executes(c -> {
          User user = getUserSender(c);
          TeleportRequest first = TeleportRequests.latestIncoming(user);

          if (first == null) {
            throw TpExceptions.NO_TP_REQUESTS;
          }

          first.deny();
          return 0;
        });
  }
}