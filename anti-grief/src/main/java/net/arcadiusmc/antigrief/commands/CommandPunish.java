package net.arcadiusmc.antigrief.commands;

import net.arcadiusmc.antigrief.ui.AdminUi;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandPunish extends FtcCommand {

  public CommandPunish() {
    super("Punish");

    setDescription("Opens the punishment menu for a specific user");
    setAliases("p");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /Punish
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              AdminUi.open(user, target);
              return 0;
            })
        );
  }
}