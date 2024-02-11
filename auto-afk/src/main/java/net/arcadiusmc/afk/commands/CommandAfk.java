package net.arcadiusmc.afk.commands;

import net.arcadiusmc.afk.Afk;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandAfk extends BaseCommand {

  public CommandAfk() {
    super("afk");

    setDescription("Marks or un-marks you as AFK");
    setAliases("away");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Sets you afk/unafk");
    factory.usage("<message>", "AFKs you with an AFK message");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> afk(getUserSender(c), null))

        .then(literal("-other")
            .requires(s -> s.hasPermission(getAdminPermission()))

            .then(argument("user", Arguments.ONLINE_USER)
                .requires(s -> s.hasPermission(getAdminPermission()))

                .executes(c -> afk(
                    Arguments.getUser(c, "user"),
                    null
                ))
            )
        )

        .then(argument("msg", Arguments.MESSAGE)
            .executes(c -> afk(
                getUserSender(c),
                Arguments.getPlayerMessage(c, "msg")
            ))
        );
  }

  private int afk(User user, PlayerMessage message) {
    if (Afk.isAfk(user)) {
      Afk.unafk(user);
    } else {
      Afk.afk(user, message);
    }

    return 0;
  }
}