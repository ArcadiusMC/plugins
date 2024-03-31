package net.arcadiusmc.markets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

class SimpleMarketCommand extends BaseCommand {

  private final CommandFunction function;

  SimpleMarketCommand(String name, CommandFunction function) {
    super(name);
    this.function = function;
    simpleUsages();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);
      Market market = Markets.getOwned(user);

      if (market == null) {
        throw MExceptions.noMarketOwned(user);
      }

      function.run(user, market);
      return 0;
    });
  }

  interface CommandFunction {

    void run(User user, Market market) throws CommandSyntaxException;
  }
}
