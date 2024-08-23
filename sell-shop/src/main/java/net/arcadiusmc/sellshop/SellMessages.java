package net.arcadiusmc.sellshop;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.UnitFormat;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface SellMessages {

  static Component soldItems(Audience viewer, SellResult result, Material material) {
    return soldItems(viewer, result.getSold(), result.getEarned(), material);
  }

  static Component soldItemsTotal(Audience viewer, int sold, int earned, Material material) {
    return Messages.render("sellshop.sold.total")
        .addValue("item", new ItemStack(material, sold))
        .addValue("earned", UnitFormat.currency(earned))
        .addValue("sold", sold)
        .addValue("material", material)
        .create(viewer);
  }

  static Component soldItems(Audience viewer, int sold, int earned, Material material) {
    return Messages.render("sellshop.sold.single")
        .addValue("item", new ItemStack(material, sold))
        .addValue("earned", UnitFormat.currency(earned))
        .addValue("sold", sold)
        .addValue("material", material)
        .create(viewer);
  }

  static Component priceDropped(Audience viewer, Material material, int before, int after) {
    return Messages.render("sellshop.priceDrop")
        .addValue("material", material)
        .addValue("before", UnitFormat.currency(before))
        .addValue("after", UnitFormat.currency(after))
        .create(viewer);
  }

}
