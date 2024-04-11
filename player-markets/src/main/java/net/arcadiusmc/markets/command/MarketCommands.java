package net.arcadiusmc.markets.command;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.gui.ShopEditBook;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.kyori.adventure.inventory.Book;

public class MarketCommands {

  static MarketArgument argument;

  public static void registerAll(MarketsPlugin plugin) {
    argument = new MarketArgument(plugin);

    new CommandMarketBounds(plugin);
    new CommandMergeShop();
    new CommandMarketsList(plugin.getLists());
    new CommandMarketDescription();

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandMarkets(plugin));

    MarketTargetCommand trust = new MarketTargetCommand(
        "shoptrust",
        (user, target, market) -> {
          boolean wasAdded = market.getMembers().add(target.getUniqueId());

          if (!wasAdded) {
            throw Messages.render("markets.errors.alreadyTrusted")
                .addValue("player", target)
                .exception(user);
          }

          user.sendMessage(
              Messages.render("markets.trusted")
                  .addValue("player", target)
                  .create(user)
          );
        }
    );
    trust.setAliases("shop-trust");
    trust.setDescription("Trusts a player in your shop");
    trust.register();

    MarketTargetCommand untrust = new MarketTargetCommand(
        "shopuntrust",
        (user, target, market) -> {
          boolean wasRemoved = market.getMembers().remove(target.getUniqueId());

          if (!wasRemoved) {
            throw Messages.render("markets.errors.notTrusted")
                .addValue("player", target)
                .exception(user);
          }

          user.sendMessage(
              Messages.render("markets.untrusted")
                  .addValue("player", target)
                  .create(user)
          );
        }
    );
    untrust.setAliases("shop-untrust");
    untrust.setDescription("Untrusts a player from your shop");
    untrust.register();

    MarketTargetCommand ban = new MarketTargetCommand(
        "shopban",
        (user, target, market) -> {
          boolean wasAdded = market.getBannedCustomers().add(target.getUniqueId());

          if (!wasAdded) {
            throw Messages.render("markets.errors.alreadyBanned")
                .addValue("player", target)
                .exception(user);
          }

          user.sendMessage(
              Messages.render("markets.banned")
                  .addValue("player", target)
                  .create(user)
          );
        }
    );
    ban.setAliases("shop-ban");
    ban.setDescription("Bans a player from your shop");
    ban.register();

    MarketTargetCommand unban = new MarketTargetCommand(
        "shopunban",
        (user, target, market) -> {
          boolean wasRemoved = market.getBannedCustomers().remove(target.getUniqueId());

          if (!wasRemoved) {
            throw Messages.render("markets.errors.notBanned")
                .addValue("player", target)
                .exception(user);
          }

          user.sendMessage(
              Messages.render("markets.unbanned")
                  .addValue("player", target)
                  .create(user)
          );
        }
    );
    unban.setAliases("shop-unban");
    unban.setDescription("Unbans a player from your shop");
    unban.register();

    MarketTargetCommand transfer = new MarketTargetCommand(
        "transfershop", new TransferShopFunction()
    );
    transfer.setAliases("transfer-shop");
    transfer.setDescription("Transfer a shop to another player");
    transfer.register();

    SimpleMarketCommand unmerge = new SimpleMarketCommand(
        "unmergeshop",
        (user, market) -> {
          if (market.getMerged() == null) {
            throw MExceptions.notMerged(user);
          }

          market.unmerge();

          user.sendMessage(Messages.renderText("markets.merged.unmerged", user));
        }
    );
    unmerge.setDescription("Unmerge your shop");
    unmerge.setAliases("unmerge-shop");
    unmerge.register();

    SimpleMarketCommand unclaim = new SimpleMarketCommand("unclaimshop", new UnclaimShopFunction());
    unclaim.setAliases("unclaim-shop");
    unclaim.setDescription("Unclaim your shop");
    unclaim.register();

    SimpleMarketCommand gui = new SimpleMarketCommand(
        "marketgui",
        (user, market) -> {
          Book book = ShopEditBook.createBook(market, user);
          user.openBook(book);
        }
    );
    gui.setDescription("Opens the player shop GUI");
    gui.setAliases("marketmenu", "market-menu", "shopgui", "shop-gui");
    gui.register();
  }
}
