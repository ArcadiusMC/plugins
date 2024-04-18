package net.arcadiusmc.markets.command;

import java.util.UUID;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.CurrencyCommand;
import net.arcadiusmc.command.UserMapTopCommand;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.gui.ShopEditBook;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.utils.ScoreIntMap;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;

public class MarketCommands {

  static MarketArgument argument;
  static MarketListArgument listArgument;

  public static void registerAll(MarketsPlugin plugin) {
    argument = new MarketArgument(plugin);
    listArgument = new MarketListArgument(plugin, argument);

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


    // -- Debt commands --
    ScoreIntMap<UUID> debts = plugin.getDebts().getDebts();
    // Registers itself
    new UserMapTopCommand("debttop", debts, UnitFormat::currency, Component.text("Debt Top"));

    UserService service = Users.getService();
    service.getCurrencies().get("balances").ifPresent(currency -> {
      DebtCurrency debtCurrency = new DebtCurrency(debts, currency);
      new CurrencyCommand("debt", debtCurrency);
    });

    // -- Modifier commands --
    ModifierListCommand priceMod = new ModifierListCommand(
        plugin,
        "market-price-modifiers",
        Market::getPriceModifiers,
        "markets.priceModifiers"
    );
    priceMod.setAliases("shop-price-mods", "market-price-mods");
    priceMod.setDescription("Player shop price modifier managing.");
    priceMod.register();

    ModifierListCommand rentMod = new ModifierListCommand(
        plugin,
        "market-rent-modifiers",
        Market::getRentModifiers,
        "markets.rentModifiers"
    );
    rentMod.setAliases("shop-rent-mods", "market-rent-mods");
    rentMod.setDescription("Player shop rent modifier managing.");
    rentMod.register();

    ModifierListCommand taxMod = new ModifierListCommand(
        plugin,
        "market-tax-modifiers",
        Market::getRentModifiers,
        "markets.taxModifiers"
    );
    taxMod.setAliases("shop-tax-mods", "market-tax-mods");
    taxMod.setDescription("Player shop tax modifier managing.");
    taxMod.register();
  }

  record DebtCurrency(ScoreIntMap<UUID> map, Currency regularCurrency) implements Currency {

    @Override
    public String singularName() {
      return regularCurrency.singularName();
    }

    @Override
    public String pluralName() {
      return regularCurrency.pluralName();
    }

    @Override
    public Component format(int amount) {
      return regularCurrency.format(amount);
    }

    @Override
    public int get(UUID playerId) {
      return map.get(playerId);
    }

    @Override
    public void set(UUID playerId, int value) {
      map.set(playerId, value);
    }
  }
}
