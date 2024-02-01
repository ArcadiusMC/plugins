package net.arcadiusmc.core.commands;

import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.user.UserBlockList.IgnoreResult;

public class CommandIgnore extends BaseCommand {

  public CommandIgnore() {
    super("ignore");

    setPermission(CorePermissions.IGNORE);
    setAliases("ignoreplayer", "unignore", "unignoreplayer", "block", "unblock");
    setDescription("Makes you ignore/unignore another player");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>", "Ignores/unignores a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              if (target.equals(user)) {
                throw CoreExceptions.CANNOT_IGNORE_SELF.exception(user);
              }

              UserBlockList list = user.getComponent(UserBlockList.class);
              boolean alreadyIgnoring = list.testIgnored(target) == IgnoreResult.BLOCKED;

              if (alreadyIgnoring) {
                user.sendMessage(CoreMessages.unignorePlayer(target));
                list.removeIgnored(target);
              } else {
                user.sendMessage(CoreMessages.ignorePlayer(target));
                list.setIgnored(target, false);
              }

              return 0;
            })
        );
  }
}
