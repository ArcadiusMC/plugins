package net.arcadiusmc.economy.market.commands;

import static net.arcadiusmc.economy.EconMessages.STRUST_BLOCKED_SENDER;
import static net.arcadiusmc.economy.EconMessages.STRUST_BLOCKED_TARGET;

import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.economy.EconExceptions;
import net.arcadiusmc.economy.EconMessages;
import net.arcadiusmc.economy.EconPermissions;
import net.arcadiusmc.economy.ShopsPlugin;
import net.arcadiusmc.economy.market.MarketManager;
import net.arcadiusmc.economy.market.MarketShop;
import net.arcadiusmc.economy.market.Markets;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;

public class CommandShopTrust extends FtcCommand {

  public CommandShopTrust() {
    super("shoptrust");

    setPermission(EconPermissions.MARKETS);
    setAliases("shopuntrust", "markettrust", "marketuntrust");
    setDescription("Trusts/untrusts a player in your shop");

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /shoptrust <user>
   *
   * Permissions used:
   * ftc.markets
   *
   * Main Author: Julie :D
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>")
        .addInfo("Trusts/untrusts a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              if (user.equals(target)) {
                throw Exceptions.format("Cannot trust self");
              }

              if (!Markets.ownsShop(user)) {
                throw EconExceptions.NO_SHOP_OWNED;
              }

              MarketManager region = ShopsPlugin.getPlugin().getMarkets();
              MarketShop shop = region.get(user.getUniqueId());

              boolean trusted = shop.getMembers().contains(target.getUniqueId());

              if (trusted) {
                shop.untrust(target.getUniqueId());

                user.sendMessage(EconMessages.shopUntrustSender(target));
                target.sendMessage(EconMessages.shopUntrustTarget(user));
              } else {
                UserBlockList.testBlockedException(user, target,
                    STRUST_BLOCKED_SENDER,
                    STRUST_BLOCKED_TARGET
                );

                shop.trust(target.getUniqueId());

                user.sendMessage(EconMessages.shopTrustSender(target));
                target.sendMessage(EconMessages.shopTrustTarget(user));
              }

              return 0;
            })
        );
  }
}