package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.lore.EnchantsLoreElement;
import net.arcadiusmc.items.lore.LoreElement;
import org.bukkit.inventory.ItemStack;

public class PickaxeItem implements ItemType {

  static final int MAX_LEVEL = 10;

  @Override
  public ItemStack createBaseItem() {
    return null;
  }

  @Override
  public void addComponents(ExtendedItem item) {
    Level level = new Level(MAX_LEVEL);

    item.addComponent(level);
    item.addComponent(new Owner());

    item.addLore(EnchantsLoreElement.ENCHANTS);
    item.addLore(LoreElement.EMPTY_LINE);
    item.addLore(level);
    item.addLore(LoreElement.BORDER);
    item.addLore(CraftedForLore.ELEMENT);
  }
}
