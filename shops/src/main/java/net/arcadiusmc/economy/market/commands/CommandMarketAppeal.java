package net.arcadiusmc.economy.market.commands;

import static net.arcadiusmc.economy.market.MarketEviction.SOURCE_AUTOMATIC;

import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.economy.EconExceptions;
import net.arcadiusmc.economy.EconMessages;
import net.arcadiusmc.economy.EconPermissions;
import net.arcadiusmc.economy.ShopsPlugin;
import net.arcadiusmc.economy.market.MarketManager;
import net.arcadiusmc.economy.market.MarketScan;
import net.arcadiusmc.economy.market.MarketShop;
import net.arcadiusmc.economy.market.Markets;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandMarketAppeal extends FtcCommand {

  public CommandMarketAppeal() {
    super("MarketAppeal");

    setPermission(EconPermissions.MARKETS);
    setDescription("Appeals an automated market eviction");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /MarketAppeal
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);

          if (!Markets.ownsShop(user)) {
            throw EconExceptions.NO_SHOP_OWNED;
          }

          var plugin = ShopsPlugin.getPlugin();
          var config = plugin.getShopConfig();
          MarketManager markets = plugin.getMarkets();

          MarketShop shop = markets.get(user.getUniqueId());

          if (!shop.markedForEviction()) {
            throw EconExceptions.NOT_MARKED_EVICTION;
          }

          var eviction = shop.getEviction();

          if (!eviction.getSource().equals(SOURCE_AUTOMATIC)) {
            throw EconExceptions.NON_AUTO_APPEAL;
          }

          MarketScan scan = MarketScan.create(Markets.getWorld(), shop, plugin.getShops());
          int total = scan.stockedCount() + scan.unstockedCount();
          float required = config.getMinStock() * total;

          if (total < config.getMinimumShopAmount()) {
            user.sendMessage(EconMessages.cannotAppeal(EconMessages.tooLittleShops()));
            return 0;
          }

          if (scan.stockedCount() < required) {
            user.sendMessage(EconMessages.cannotAppeal(EconMessages.MARKET_EVICT_STOCK));
            return 0;
          }

          shop.stopEviction();
          user.sendMessage(EconMessages.MARKET_APPEALED_EVICTION);

          return 0;
        });
  }
}