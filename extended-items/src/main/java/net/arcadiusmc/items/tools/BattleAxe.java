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
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class BattleAxe implements ItemType {

  static final int MAX_LEVEL = 10;

  static final ItemUpgrades UPGRADES = ItemUpgrades.builder()
      .previewPrefix(LoreElement.DOUBLE_EMPTY_LINE)
      .statusPrefix(LoreElement.DOUBLE_EMPTY_LINE)

      .level(1, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 1));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.SOULBOUND, 1));
        level.upgrade(new ModelDataMod(10050001));
      })
      .level(2, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 2));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.CUTTING_MASTERY, 1));
      })
      .level(3, level -> {
        level.upgrade(new ItemTypeMod(Material.STONE_AXE));
        level.upgrade(new ModelDataMod(10050002));
      })
      .level(4, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 3));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.CUTTING_MASTERY, 2));
      })
      .level(5, level -> {
        level.upgrade(new ItemTypeMod(Material.IRON_AXE));
        level.upgrade(new ModelDataMod(10050003));
      })
      .level(6, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 4));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.CUTTING_MASTERY, 3));
      })
      .level(7, level -> {
        level.upgrade(new ItemTypeMod(Material.DIAMOND_SHOVEL));
        level.upgrade(new ModelDataMod(10050004));
      })
      .level(8, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 5));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.CUTTING_MASTERY, 4));
      })
      .level(9, level -> {
        level.upgrade(new ItemTypeMod(Material.NETHERITE_SHOVEL));
        level.upgrade(new ModelDataMod(10050005));
      })
      .level(10, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 6));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.CUTTING_MASTERY, 5));
      })
      .build();

  static final ItemGoals GOALS = ItemGoals.builder()
      .prefixedWith(LoreElement.BORDER)

      .level(1, level -> {
        level.add(Goal.blocksBroken(Material.OAK_LOG, 100));
      })
      .level(2, level -> {
        level.add(Goal.blocksBroken(Material.OAK_LOG, 150));
      })
      .level(3, level -> {
        level.add(Goal.blocksBroken(Material.BIRCH_LOG, 1000));
        level.add(Goal.blocksBroken(Material.SPRUCE_LOG, 1000));
      })
      .level(4, level -> {
        level.add(Goal.blocksBroken(Material.BIRCH_LOG, 1500));
        level.add(Goal.blocksBroken(Material.SPRUCE_LOG, 1500));
      })
      .level(5, level -> {
        level.add(Goal.blocksBroken(Material.ACACIA_LOG, 1000));
        level.add(Goal.blocksBroken(Material.CHERRY_LOG, 1000));
      })
      .level(6, level -> {
        level.add(Goal.blocksBroken(Material.ACACIA_LOG, 1500));
        level.add(Goal.blocksBroken(Material.CHERRY_LOG, 1500));
      })
      .level(7, level -> {
        level.add(Goal.blocksBroken(Material.MANGROVE_LOG, 1000));
        level.add(Goal.blocksBroken(Material.CRIMSON_STEM, 500));
        level.add(Goal.blocksBroken(Material.WARPED_STEM, 500));
      })
      .level(8, level -> {
        level.add(Goal.blocksBroken(Material.MANGROVE_LOG, 1500));
        level.add(Goal.blocksBroken(Material.CRIMSON_STEM, 750));
        level.add(Goal.blocksBroken(Material.WARPED_STEM, 750));
      })
      .level(8, level -> {
        level.add(Goal.blocksBroken(Material.OAK_LOG, 500));
        level.add(Goal.blocksBroken(Material.BIRCH_LOG, 500));
        level.add(Goal.blocksBroken(Material.SPRUCE_LOG, 500));
        level.add(Goal.blocksBroken(Material.ACACIA_LOG, 500));
        level.add(Goal.blocksBroken(Material.CHERRY_LOG, 500));
        level.add(Goal.blocksBroken(Material.MANGROVE_LOG, 500));
        level.add(Goal.blocksBroken(Material.CRIMSON_STEM, 250));
        level.add(Goal.blocksBroken(Material.WARPED_STEM, 250));
      })

      .build();

  @Override
  public ItemStack createBaseItem() {
    return ItemStacks.builder(Material.WOODEN_AXE)
        .setUnbreakable(true)
        .editMeta(meta -> {
          meta.itemName(Text.valueOf("&7&l[<gradient=gold,yellow:Woodcutter's Axe>&7&l]"));
        })
        .build();
  }

  @Override
  public void addComponents(ExtendedItem item) {
    ToolItem.configure(item, MAX_LEVEL, UPGRADES, GOALS);
  }
}
