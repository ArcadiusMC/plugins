package net.arcadiusmc.signshops;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.math.WorldVec3i;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public interface SMessages {

  static Component noExample(Audience viewer) {
    return Messages.renderText("signshops.errors.noExample", viewer);
  }

  static Component shopCreateFailed(Audience viewer) {
    return Messages.renderText("signshops.errors.createFailed", viewer);
  }

  static Component historyTitle(Audience viewer) {
    return Messages.renderText("signshops.history.title", viewer);
  }

  static Component cannotDestroy(Audience viewer) {
    return Messages.renderText("signshops.errors.cannotDestroy", viewer);
  }

  private static String suffix(ShopType type) {
    return type.isBuyType() ? "buy" : "sell";
  }

  static Component displayName(Audience viewer, ShopType type) {
    return Messages.render("signshops.types." + type.name().toLowerCase()).create(viewer);
  }

  static Component setShopType(Audience viewer, ShopType type) {
    return Messages.render("signshops.edit.type")
        .addValue("type", displayName(viewer, type))
        .create(viewer);
  }

  static Component shopEditAmount(Audience viewer, ItemStack exampleItem) {
    return Messages.render("signshops.edit.amount")
        .addValue("item", exampleItem)
        .create(viewer);
  }

  static Component shopEditPrice(Audience viewer, int price) {
    return Messages.render("signshops.edit.price")
        .addValue("price", UnitFormat.currency(price))
        .create(viewer);
  }

  static Component sessionEndOwner(Audience viewer, SignShopSession session) {
    return sessionEnd(viewer, session, true);
  }

  static Component sessionEndCustomer(Audience viewer, SignShopSession session) {
    return sessionEnd(viewer, session, false);
  }

  private static Component sessionEnd(Audience viewer, SignShopSession session, boolean owner) {
    ItemStack item = session.getExampleItem();
    item.setAmount(session.getAmount());

    String suffix = owner ? "owner" : "customer";

    return Messages.render("signshops.session.end", suffix, suffix(session.getType()))
        .addValue("price", UnitFormat.currency(session.getPrice()))
        .addValue("earned", UnitFormat.currency(session.getTotalEarned()))
        .addValue("item", item)
        .addValue("customer", session.getCustomer())
        .addValue("location", session.getShop().getPosition().toLocation())
        .create(viewer);
  }

  static Component stockIssueMessage(Audience viewer, ShopType type, WorldVec3i pos) {
    return Messages.render("signshops.errors.stockIssue", suffix(type))
        .addValue("location", pos.toLocation())
        .create(viewer);
  }

  static Component sessionInteraction(Audience viewer, SignShopSession session) {
    ShopType type = session.getType();
    return Messages.render("signshops.session.interaction", suffix(type))
        .addValue("item", session.getExampleItem())
        .addValue("price", UnitFormat.currency(session.getPrice()))
        .create(viewer);
  }

  static Component createdShop(Audience viewer, SignShop shop) {
    return Messages.render("signshops.created", suffix(shop.getType()))
        .addValue("item", shop.getExampleItem())
        .addValue("price", UnitFormat.currency(shop.getPrice()))
        .create(viewer);
  }

  static Component formatShopHistory(Audience viewer, HistoryEntry entry, ItemStack exampleItem) {
    ItemStack item = exampleItem.clone();
    item.setAmount(entry.amount());

    return Messages.render("signshops.history.entry.", entry.wasBuy() ? "buy" : "sell")
        .addValue("player", Users.get(entry.customer()))
        .addValue("item", item)
        .addValue("price", UnitFormat.currency(entry.earned()))
        .create(viewer);
  }

  static Component nonResellableItem(Audience viewer, ItemStack item) {
    return Messages.render("signshops.errors.nonResellableItem")
        .addValue("item", item)
        .create(viewer);
  }

  static Component resellToggle(Audience viewer, boolean state) {
    return Messages.render("signshops.reselling", state ? "on" : "off").create(viewer);
  }

  static Component setLine(Audience viewer, int line, Component text) {
    return Messages.render("signshops.edit.setLine")
        .addValue("line", line)
        .addValue("text", text)
        .create(viewer);
  }
}
