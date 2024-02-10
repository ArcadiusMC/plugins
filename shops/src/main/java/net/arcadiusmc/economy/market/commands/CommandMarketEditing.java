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
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;

public class CommandMarketEditing extends FtcCommand {

  public CommandMarketEditing() {
    super("MarketEditing");

    setAliases("toggleshopediting", "togglemarketediting");
    setPermission(EconPermissions.MARKETS);
    setDescription("Allows/disallows shop members to edit sign shops in your shop");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /MarketEditing
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);

      if (!Markets.ownsShop(user)) {
        throw EconExceptions.NO_SHOP_OWNED;
      }

      MarketManager markets = ShopsPlugin.getPlugin().getMarkets();
      MarketShop shop = markets.get(user.getUniqueId());

      boolean state = !shop.isMemberEditingAllowed();
      shop.setMemberEditingAllowed(state);

      user.sendMessage(Messages.toggleMessage(EconMessages.MEMBER_EDIT_FORMAT, state));
      return 0;
    });
  }
}