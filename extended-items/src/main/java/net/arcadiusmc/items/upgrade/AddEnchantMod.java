package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.text.TextWriter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record AddEnchantMod(Enchantment enchantment, int level) implements UpgradeFunction {

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    meta.addEnchant(enchantment, level, true);
  }

  @Override
  public void writePreview(ExtendedItem item, TextWriter writer) {
    writer.line(enchantment.displayName(level));
  }
}
