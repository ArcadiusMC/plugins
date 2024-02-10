package net.arcadiusmc.marriages.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.FtcCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.marriages.MExceptions;
import net.arcadiusmc.marriages.MMessages;
import net.arcadiusmc.marriages.MPermissions;
import net.arcadiusmc.marriages.Marriages;
import net.arcadiusmc.user.User;

public class CommandDivorce extends FtcCommand {

  public CommandDivorce() {
    super("divorce");

    setPermission(MPermissions.MARRY);
    setDescription("Divorces your spouse");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /divorce
   *
   * Permissions used:
   * ftc.marry
   *
   * Main Author: Julie
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          User spouse = Marriages.getSpouse(user);

          if (spouse == null) {
            throw MExceptions.NOT_MARRIED;
          }

          user.sendMessage(MMessages.confirmDivorce(spouse));
          return 0;
        })

        .then(literal("confirm")
            .executes(c -> {
              User user = getUserSender(c);
              testCanDivorce(user);
              Marriages.divorce(user);
              return 0;
            })
        );
  }

  public static void testCanDivorce(User user) throws CommandSyntaxException {
    if (Marriages.getSpouse(user) != null) {
      return;
    }

    throw MExceptions.NOT_MARRIED;
  }
}