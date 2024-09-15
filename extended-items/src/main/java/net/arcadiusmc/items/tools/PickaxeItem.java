package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.goal.Goal;
import net.arcadiusmc.items.goal.ItemGoals;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.AddEnchantMod;
import net.arcadiusmc.items.upgrade.ItemTypeMod;
import net.arcadiusmc.items.upgrade.ItemUpgrades;
import net.arcadiusmc.items.upgrade.ModelDataMod;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class PickaxeItem implements ItemType {

  static final int MAX_LEVEL = 10;

  static final ItemUpgrades UPGRADES = ItemUpgrades.builder()
      .previewPrefix(LoreElement.DOUBLE_EMPTY_LINE)
      .statusPrefix(LoreElement.DOUBLE_EMPTY_LINE)

      .level(1, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.SOULBOUND, 1));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 1));
        level.upgrade(new ModelDataMod(10060001));
      })
      .level(2, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.IMPERIAL_DUPING, 1));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 2));
      })
      .level(3, level -> {
        level.upgrade(new ItemTypeMod(Material.STONE_PICKAXE));
        level.upgrade(new ModelDataMod(10060002));
      })
      .level(4, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.IMPERIAL_DUPING, 2));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 3));
      })
      .level(5, level -> {
        level.upgrade(new ItemTypeMod(Material.IRON_PICKAXE));
        level.upgrade(new ModelDataMod(10060003));
      })
      .level(6, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.IMPERIAL_DUPING, 3));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 4));
      })
      .level(7, level -> {
        level.upgrade(new ItemTypeMod(Material.DIAMOND_PICKAXE));
        level.upgrade(new ModelDataMod(10060004));
      })
      .level(8, level -> {
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.IMPERIAL_DUPING, 4));
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 5));
      })
      .level(9, level -> {
        level.upgrade(new ItemTypeMod(Material.NETHERITE_PICKAXE));
        level.upgrade(new ModelDataMod(10060005));
      })
      .level(10, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 6));
      })
      .build();

  static final ItemGoals GOALS = ItemGoals.builder()
      .prefixedWith(LoreElement.BORDER)

      .level(1, level -> {
        level.add(Goal.blocksBroken(Material.STONE, 1000));
      })
      .level(2, level -> {
        level.add(Goal.blocksBroken(Material.STONE, 2000));
      })
      .level(3, level -> {
        level.add(Goal.blocksBroken(Material.ANDESITE, 1000));
        level.add(Goal.blocksBroken(Material.GRANITE, 1000));
      })
      .level(4, level -> {
        level.add(Goal.blocksBroken(Material.ANDESITE, 2000));
        level.add(Goal.blocksBroken(Material.GRANITE, 2000));
      })
      .level(5, level -> {
        level.add(Goal.blocksBroken(Material.CALCITE, 1000));
        level.add(Goal.blocksBroken(Material.DEEPSLATE, 1000));
      })
      .level(6, level -> {
        level.add(Goal.blocksBroken(Material.CALCITE, 2000));
        level.add(Goal.blocksBroken(Material.DEEPSLATE, 2000));
        level.add(Goal.DONATOR);
      })
      .level(7, level -> {
        level.add(Goal.blocksBroken(Material.TUFF, 1000));
        level.add(Goal.blocksBroken(Material.NETHERRACK, 2000));
      })
      .level(8, level -> {
        level.add(Goal.blocksBroken(Material.TUFF, 1500));
        level.add(Goal.blocksBroken(Material.NETHERRACK, 3000));
      })
      .level(9, level -> {
        level.add(Goal.blocksBroken(Material.STONE, 1000));
        level.add(Goal.blocksBroken(Material.ANDESITE, 1000));
        level.add(Goal.blocksBroken(Material.GRANITE, 1000));
        level.add(Goal.blocksBroken(Material.DEEPSLATE, 1000));
        level.add(Goal.blocksBroken(Material.NETHERRACK, 2000));
        level.add(Goal.blocksBroken(Material.OBSIDIAN, 100));
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
    ToolItem.configure(item, MAX_LEVEL, UPGRADES, GOALS);
  }
}
