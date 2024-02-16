package net.arcadiusmc.punish.commands;


import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.user.UserBlockList.IgnoreResult;
import net.forthecrown.grenadier.GrenadierCommand;
import org.slf4j.Logger;

class CommandSeparate extends BaseCommand {

  private static final Logger LOGGER = Loggers.getLogger("Separation");

  public CommandSeparate() {
    super("seperate");
    setDescription("Seperates/unseparates 2 players");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user 1> <user 2>", "Separates/unseparates 2 players");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("first", Arguments.USER)
            .then(argument("second", Arguments.USER)
                .executes(c -> {
                  User first = Arguments.getUser(c, "first");
                  User second = Arguments.getUser(c, "second");

                  UserBlockList firstInter = first.getComponent(UserBlockList.class);
                  UserBlockList secondInter = second.getComponent(UserBlockList.class);

                  // Both users' lists separated lists have to contain
                  // the other player, just to make sure
                  if (firstInter.testIgnored(second) == IgnoreResult.SEPARATED
                      && secondInter.testIgnored(first) == IgnoreResult.SEPARATED
                  ) {
                    firstInter.removeSeparated(second);
                    secondInter.removeSeparated(first);

                    c.getSource().sendSuccess(
                        Messages.render("cmd.separate.unseparated")
                            .addValue("player1", first)
                            .addValue("player2", second)
                            .create(c.getSource())
                    );

                    LOGGER.info("{} un-separated {} and {}",
                        c.getSource().textName(), first, second
                    );
                  } else {
                    firstInter.setIgnored(second, true);
                    secondInter.setIgnored(first, true);

                    c.getSource().sendSuccess(
                        Messages.render("cmd.separate.separated")
                            .addValue("player1", first)
                            .addValue("player2", second)
                            .create(c.getSource())
                    );

                    LOGGER.info("{} separated {} and {}",
                        c.getSource().textName(), first, second
                    );
                  }

                  return 0;
                })
            )
        );
  }
}