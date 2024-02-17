package net.arcadiusmc.signshops;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.UnitFormat;
import net.kyori.adventure.audience.Audience;
import org.bukkit.inventory.ItemStack;

public interface SExceptions {
  static CommandSyntaxException outOfStock(Audience viewer) {
    return Messages.render("signshops.errors.outOfStock").exception(viewer);
  }

  static CommandSyntaxException shopNoSpace(Audience viewer) {
    return Messages.render("signshops.errors.shopNoSpace").exception(viewer);
  }

  static CommandSyntaxException noDescription(Audience viewer) {
    return Messages.render("signshops.errors.noDescription").exception(viewer);
  }

  static CommandSyntaxException noPrice(Audience viewer) {
    return Messages.render("signshops.errors.noPrice").exception(viewer);
  }

  static CommandSyntaxException lookAtShop(Audience viewer) {
    return Messages.render("signshops.errors.lookAtShop").exception(viewer);
  }

  static CommandSyntaxException invalidShop(Audience viewer) {
    return Messages.render("signshops.errors.invalidShop").exception(viewer);
  }

  static CommandSyntaxException transferSelf(Audience viewer) {
    return Messages.render("signshops.errors.transferSelf").exception(viewer);
  }

  static CommandSyntaxException noItemToSell(Audience viewer, ItemStack item) {
    return Messages.render("signshops.errors.noItemToSell")
        .addValue("item", item)
        .exception(viewer);
  }

  static CommandSyntaxException ownerCannotAfford(Audience viewer, int amount) {
    return Messages.render("signshops.errors.ownerCannotAfford")
        .addValue("amount", UnitFormat.currency(amount))
        .exception(viewer);
  }

  static CommandSyntaxException shopMaxPrice(Audience viewer) {
    int max = SignShopsPlugin.plugin().getShopConfig().maxPrice();

    return Messages.render("signshops.errors.shopMaxPrice")
        .addValue("max", UnitFormat.currency(max))
        .exception(viewer);
  }

  static CommandSyntaxException shopNotAllowed(Audience viewer) {
    return Messages.render("signshops.errors.shopNotAllowed").exception(viewer);
  }
}
