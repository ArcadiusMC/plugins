package net.arcadiusmc.markets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

class MarketTargetCommand extends BaseCommand {

  private final TargetFunction function;

  MarketTargetCommand(String name, TargetFunction function) {
    super(name);
    this.function = function;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>").addInfo(getDescription());
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              Market market = Markets.getOwned(user);

              if (market == null) {
                throw MExceptions.noMarketOwned(user);
              }

              function.run(user, target, market);
              return 0;
            })
        );
  }

  interface TargetFunction {
    void run(User user, User target, Market market) throws CommandSyntaxException;
  }
}
