package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.AddEnchantMod;
import net.arcadiusmc.items.upgrade.ItemUpgrades;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class PickaxeItem implements ItemType {

  static final int MAX_LEVEL = 10;

  static final ItemUpgrades UPGRADES = ItemUpgrades.builder()
      .level(1, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.SOULBOUND, 1));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 1));
      })
      .build();

  @Override
  public ItemStack createBaseItem() {
    return ItemStacks.builder(Material.WOODEN_PICKAXE)
        .setUnbreakable(true)
        .editMeta(meta -> {
          meta.itemName(Messages.renderText("itemsPlugin.pickaxe.name"));
        })
        .build();
  }

  @Override
  public void addComponents(ExtendedItem item) {
    Level level = new Level(MAX_LEVEL);

    item.addComponent(level);
    item.addComponent(new Owner());
    item.addComponent(UPGRADES.createComponent());

    item.addLore(LoreElement.SINGLE_EMPTY_LINE);
    item.addLore(level);
    item.addLore(LoreElement.BORDER);
    item.addLore(CraftedForLore.ELEMENT);
  }
}
