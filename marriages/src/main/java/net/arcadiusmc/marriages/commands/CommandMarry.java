package net.arcadiusmc.marriages.commands;

import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.marriages.MPermissions;
import net.arcadiusmc.marriages.requests.Proposals;
import net.arcadiusmc.user.User;

public class CommandMarry extends FtcCommand {

  public CommandMarry() {
    super("marry");

    setDescription("Marry a person");
    setPermission(MPermissions.MARRY);
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /marry
   *
   * Permissions used:
   * ftc.marry
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>", "Propose to a <player>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              Proposals.propose(user, target);
              return 0;
            })
        );
  }
}