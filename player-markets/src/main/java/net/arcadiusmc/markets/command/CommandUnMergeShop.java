package net.arcadiusmc.markets.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandUnMergeShop extends BaseCommand {

  public CommandUnMergeShop() {
    super("unmerge-market");

    setAliases("unmerge", "unmergeshop", "unmergemarket", "unmerge-shop");
    setDescription("Unmerges your owned shop");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);
      Market market = Markets.getOwned(user);

      if (market == null) {
        throw MExceptions.noMarketOwned(user);
      }

      if (market.getMerged() == null) {
        throw MExceptions.notMerged(user);
      }

      market.unmerge();

      user.sendMessage(Messages.renderText("markets.merged.unmerged", user));
      return 0;
    });
  }
}
