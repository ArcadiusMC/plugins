package net.arcadiusmc.markets.gui;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.math.BlockVector3;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.MarketsConfig;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.event.MarketPurchaseAttemptEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.text.UserClickCallback;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.BookBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.BookMeta;
import org.spongepowered.math.vector.Vector3i;

public final class ClaimingBook {
  private ClaimingBook() {}

  public static BookMeta create(Market market, User user) {
    BookBuilder builder = new BookBuilder();
    Component title = Messages.renderText("markets.gui.title.claiming", user);

    builder.title(title)
        .author(Component.text(Bukkit.getName()))
        .addCentered(title);

    int price = market.getPriceFor(user);
    int rent = market.getBaseRent();
    int entrances = market.getEntrances().size();

    Vector3i dimensions = market.getRegion()
        .map(r -> {
          BlockVector3 dif = r.getMaximumPoint().subtract(r.getMinimumPoint());
          return Vector3i.from(dif.getX(), dif.getY(), dif.getZ());
        })
        .orElse(Vector3i.ZERO);

    builder.addText(
        Messages.render("markets.gui.content.claiming")
            .addValue("rent", UnitFormat.currency(rent))
            .addValue("price", UnitFormat.currency(price))
            .addValue("entranceCount", entrances)
            .addValue("size", dimensions)
            .addValue("rentButton", purchaseButton(market, user, price))
            .addValue("showClaimSize", ShopEditBook.showClaimSize(market, user))
            .create(user)
    );

    return builder.build();
  }

  private static Component purchaseButton(Market market, User user, int price) {
    MarketPurchaseAttemptEvent event = callPurchaseEvent(user, market, price);

    boolean canPurchase = !event.isCancelled();
    Component denyReason = event.getDenyReason();

    ClickEvent clickEvent = ClickEvent.callback(
        (UserClickCallback) user1 -> attemptPurchase(market, user1, price),
        builder -> builder.uses(1)
    );

    String key = canPurchase ? "available" : "unavailable";

    Component result = Messages.render("markets.gui.buttons.rent", key)
        .create(user)
        .clickEvent(clickEvent);

    if (!canPurchase && !Text.isEmpty(denyReason)) {
      result = result.hoverEvent(denyReason);
    }

    return result;
  }

  private static void attemptPurchase(Market market, User user, int price)
      throws CommandSyntaxException
  {
    if (!user.hasBalance(price)) {
      throw Exceptions.cannotAfford(user, price);
    }

    MarketPurchaseAttemptEvent event = callPurchaseEvent(user, market, price);
    if (event.isCancelled()) {
      Component reason = event.getDenyReason();

      if (Text.isEmpty(reason)) {
        throw Messages.render("markets.errors.purchaseForbidden").exception(user);
      }

      throw Exceptions.create(event.getDenyReason());
    }

    MarketsManager manager = market.getManager();
    Market alreadyOwned = manager.getByOwner(user.getUniqueId());

    if (alreadyOwned != null) {
      throw Messages.render("markets.errors.alreadyOwned").exception(user);
    }

    MarketsConfig config = MarketsPlugin.plugin().getPluginConfig();

    Markets.validateActionCooldown(user);

    user.removeBalance(price);
    market.claim(user);

    user.setTimeToNow(TimeField.MARKET_LAST_ACTION);

    user.sendMessage(
        Messages.render("markets.claimed")
            .addValue("price", UnitFormat.currency(price))
            .addValue("rent", UnitFormat.currency(market.getRent()))
            .addValue("rentInterval", config.rentInterval())
            .create(user)
    );
  }

  private static MarketPurchaseAttemptEvent callPurchaseEvent(User user, Market market, int price) {
    MarketPurchaseAttemptEvent event = new MarketPurchaseAttemptEvent(user, market);

    if (!user.hasBalance(price)) {
      event.setCancelled(true);
      event.setDenyReason(Exceptions.cannotAffordText(user, price));
    }

    event.callEvent();

    return event;
  }
}