package net.arcadiusmc.markets.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.gui.ShopEditBook;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.inventory.Book;

public class CommandsMarketEditing extends BaseCommand {

  public CommandsMarketEditing() {
    super("marketediting");

    setDescription(
        "Command used by /marketgui to toggle members being able to"
            + " edit sign shops in a player shop"
    );
    setAliases("market-editing");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("state", BoolArgumentType.bool())
        .executes(c -> {
          User user = getUserSender(c);
          Market market = Markets.getOwned(user);
          boolean state = c.getArgument("state", Boolean.class);

          if (market == null) {
            throw MExceptions.noMarketOwned(user);
          }

          market.setMemberEditingAllowed(state);
          String messageKey = state ? "allowed" : "denied";

          user.sendMessage(
              Messages.render("markets.memberEditing", messageKey)
                  .create(user)
          );

          Book book = ShopEditBook.createBook(market, user);
          user.openBook(book);

          return 0;
        })
    );
  }
}
