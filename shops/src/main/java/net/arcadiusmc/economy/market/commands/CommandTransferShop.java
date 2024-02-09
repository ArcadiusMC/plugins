package net.arcadiusmc.economy.market.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.arcadiusmc.user.Users;

public class CommandTransferShop extends FtcCommand {

  public CommandTransferShop() {
    super("transfershop");

    setPermission(EconPermissions.MARKETS);
    setAliases("transfermarket", "shoptransfer", "markettransfer");
    setDescription("Transfers a player's shop in Hazelguard to another player");

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /transfershop <user>
   * /transfershop <user> <confirm>
   *
   * Permissions used:
   * ftc.markets
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>")
        .addInfo("Transfers the shop you own to a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              User user = getUserSender(c);
              User target = Arguments.getUser(c, "user");

              check(user, target);

              user.sendMessage(EconMessages.marketTransferConfirm(target));
              return 0;
            })

            .then(literal("confirm")
                .executes(c -> {
                  User user = getUserSender(c);
                  User target = Arguments.getUser(c, "user");

                  check(user, target);

                  MarketManager markets = ShopsPlugin.getPlugin().getMarkets();
                  MarketShop shop = markets.get(user.getUniqueId());

                  shop.transfer(target);

                  user.sendMessage(EconMessages.marketTransferredSender(target));
                  target.sendMessage(EconMessages.marketTransferredTarget(user));

                  return 0;
                })
            )
        );
  }

  private void check(User user, User target) throws CommandSyntaxException {
    if (!Markets.ownsShop(user)) {
      throw EconExceptions.NO_SHOP_OWNED;
    }

    if (user.equals(target)) {
      throw EconExceptions.TRANSFER_SELF;
    }

    if (Markets.ownsShop(target)) {
      throw EconExceptions.marketTargetHasShop(target);
    }

    Markets.checkStatusChange(user);

    if (!Markets.canChangeStatus(target)) {
      throw EconExceptions.marketTargetStatus(target);
    }

    var userService = Users.getService();
    if (userService.isAltAccount(target.getUniqueId())) {
      throw EconExceptions.ALTS_CANNOT_OWN;
    }
  }
}