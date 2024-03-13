package net.arcadiusmc.items;

import org.bukkit.inventory.ItemStack;

public interface ItemType {

  ItemStack createBaseItem();

  void addComponents(ExtendedItem item);

  default boolean isPersistentBeyondDeath() {
    return false;
  }
}
