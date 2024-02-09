package net.arcadiusmc.economy.market.commands;

import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.economy.EconExceptions;
import net.arcadiusmc.economy.EconMessages;
import net.arcadiusmc.economy.EconPermissions;
import net.arcadiusmc.economy.ShopsPlugin;
import net.arcadiusmc.economy.market.MarketManager;
import net.arcadiusmc.economy.market.MarketShop;
import net.arcadiusmc.economy.market.Markets;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandUnmerge extends FtcCommand {

  public CommandUnmerge() {
    super("unmerge");

    setDescription("Unmerges the shop you own with the shop it's merged with");
    setPermission(EconPermissions.MARKETS);
    setAliases("marketunmerge", "shopunmerge", "unmergeshop", "unmergemarket");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /unmerge
   *
   * Permissions used:
   * ftc.markets
   *
   * Main Author: Julie
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);

          if (!Markets.ownsShop(user)) {
            throw EconExceptions.NO_SHOP_OWNED;
          }

          MarketManager markets = ShopsPlugin.getPlugin().getMarkets();
          MarketShop shop = markets.get(user.getUniqueId());

          if (!shop.isMerged()) {
            throw EconExceptions.NOT_MERGED;
          }

          User target = shop.getMerged().ownerUser();

          shop.unmerge();

          user.sendMessage(EconMessages.marketUnmergeSender(target));
          target.sendMessage(EconMessages.marketUnmergeTarget(user));

          return 0;
        });
  }
}