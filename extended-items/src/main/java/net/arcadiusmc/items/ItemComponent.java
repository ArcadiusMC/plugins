package net.arcadiusmc.items;

import net.forthecrown.nbt.CompoundTag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class ItemComponent {

  protected ExtendedItem item;

  final void setItem(ExtendedItem item) {
    if (item == null) {
      onDetach(this.item);
    }

    this.item = item;

    if (item != null) {
      onAttach(item);
    }
  }

  protected void onInit() {

  }

  protected void onAttach(ExtendedItem item) {

  }

  protected void onDetach(ExtendedItem item) {

  }

  public void onUpdate(ItemMeta meta, ItemStack stack) {

  }

  public void save(CompoundTag tag) {

  }

  public void load(CompoundTag tag) {

  }
}
