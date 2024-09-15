package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.text.TextWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record ItemTypeMod(Material newType) implements UpgradeFunction {

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    stack.setType(newType);
  }

  @Override
  public void writePreview(ExtendedItem item, TextWriter writer) {
    writer.formattedLine("Reforge to {0}", NamedTextColor.GRAY, Component.translatable(newType));
  }
}
