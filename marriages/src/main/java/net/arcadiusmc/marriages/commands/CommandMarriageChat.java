package net.arcadiusmc.marriages.commands;

import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.marriages.MExceptions;
import net.arcadiusmc.marriages.MPermissions;
import net.arcadiusmc.marriages.Marriages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;

public class CommandMarriageChat extends FtcCommand {

  public CommandMarriageChat() {
    super("marriagechat");

    setPermission(MPermissions.MARRY);
    setAliases("marryc", "marriagec", "mc", "mchat");
    setDescription("Chat with your spouse privately");

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   *
   * Permissions used:
   * ftc.marry
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<message>", "Chats with your spouse");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("message", Arguments.MESSAGE)

            .executes(c -> {
              User user = getUserSender(c);
              var spouse = Marriages.getSpouse(user);
              PlayerMessage message = Arguments.getPlayerMessage(c, "message");

              if (spouse == null) {
                user.set(Marriages.MCHAT_TOGGLED, false);
                throw MExceptions.NOT_MARRIED;
              }

              if (!spouse.isOnline()) {
                user.set(Marriages.MCHAT_TOGGLED, false);
                throw Exceptions.notOnline(spouse);
              }

              Marriages.mchat(user, message);
              return 0;
            })
        );
  }
}