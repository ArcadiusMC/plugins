package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.text.TextWriter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface ItemUpgrade {

  void apply(ExtendedItem item, ItemMeta meta, ItemStack stack);

  default void writePreview(TextWriter writer) {

  }
}
