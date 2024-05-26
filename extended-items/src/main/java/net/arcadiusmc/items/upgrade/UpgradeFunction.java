package net.arcadiusmc.items.upgrade;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.text.TextWriter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface UpgradeFunction {

  UpgradeFunction EMPTY = (item, meta, stack) -> {};

  void apply(ExtendedItem item, ItemMeta meta, ItemStack stack);

  default void writePreview(ExtendedItem item, TextWriter writer) {

  }

  default void writeStatus(ExtendedItem item, TextWriter writer) {

  }

  /**
   * Determines if the status text of this upgrade function is kept for all proceeding levels
   * or is only used for one level.
   * <p>
   * This method is irrelevant if the {@link #writeStatus(ExtendedItem, TextWriter)} does nothing.
   *
   * @return
   */
  default boolean statusPersists() {
    return false;
  }
}
