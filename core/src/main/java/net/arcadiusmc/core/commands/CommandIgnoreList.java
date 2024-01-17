package net.arcadiusmc.core.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;

public class CommandIgnoreList extends BaseCommand {

  public CommandIgnoreList() {
    super("ignorelist");

    setPermission(CorePermissions.IGNORELIST);
    setDescription("Displays all the ignored players");
    setAliases(
        "blocked", "blockedplayers", "blockedlist",
        "ignoring", "ignored", "ignores",
        "ignoredlist", "ignoredplayers",
        "listignores", "listignored"
    );

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /<command> [user]
   *
   * Permissions used:
   * ftc.commands.ignore
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("")
        .addInfo("Shows your ignored players");

    factory.usage("<player>")
        .setPermission(CorePermissions.IGNORELIST_OTHERS)
        .addInfo("Shows a <player>'s ignored players");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> displayIgnored(c.getSource(), getUserSender(c)))

        .then(argument("user", Arguments.USER)
            .requires(s -> s.hasPermission(CorePermissions.IGNORELIST_OTHERS))

            .executes(c -> displayIgnored(c.getSource(), Arguments.getUser(c, "user")))
        );
  }

  private int displayIgnored(CommandSource source, User user) throws CommandSyntaxException {
    UserBlockList list = user.getComponent(UserBlockList.class);

    if (list.getBlocked().isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST;
    }

    source.sendMessage(CoreMessages.listBlocked(list.getBlocked(), source));
    return 0;
  }
}
