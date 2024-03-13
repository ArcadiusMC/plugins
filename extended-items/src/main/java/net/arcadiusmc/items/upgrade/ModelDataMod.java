package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record ModelDataMod(Integer modelData) implements ItemUpgrade {

  @Override
  public void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
    meta.setCustomModelData(modelData);
  }
}
