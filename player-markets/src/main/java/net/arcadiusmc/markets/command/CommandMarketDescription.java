package net.arcadiusmc.markets.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMarketDescription extends BaseCommand {

  public CommandMarketDescription() {
    super("shop-description");

    setAliases("shop-description");
    setDescription("Sets your player shop's description.");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("message", Arguments.MESSAGE)
            .executes(c -> {
              User user = getUserSender(c);
              PlayerMessage message = Arguments.getPlayerMessage(c, "message");
              Market owned = Markets.getOwned(user);

              if (owned == null) {
                throw MExceptions.noMarketOwned(user);
              }

              owned.setDescription(message);

              user.sendMessage(
                  Messages.render("markets.description.set")
                      .addValue("description", message)
                      .create(user)
              );
              return 0;
            })
        );
  }
}
