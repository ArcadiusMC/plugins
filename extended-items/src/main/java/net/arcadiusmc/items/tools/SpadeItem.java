package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.goal.Goal;
import net.arcadiusmc.items.goal.GoalKey;
import net.arcadiusmc.items.goal.ItemGoals;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.AddEnchantMod;
import net.arcadiusmc.items.upgrade.ItemTypeMod;
import net.arcadiusmc.items.upgrade.ItemUpgrades;
import net.arcadiusmc.items.upgrade.ModelDataMod;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class SpadeItem implements ItemType {

  static final int MAX_LEVEL = 10;

  public static final String GOAL_TYPE = "pirates_luck/picked_up";

  private static final ItemUpgrades UPGRADES = ItemUpgrades.builder()
      .previewPrefix(LoreElement.DOUBLE_EMPTY_LINE)
      .statusPrefix(LoreElement.DOUBLE_EMPTY_LINE)

      .level(1, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 1));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.SOULBOUND, 1));

        level.upgrade(new ModelDataMod(10040001));
      })

      .level(2, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 2));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.PIRATES_LUCK, 1));

        level.upgrade(new ItemTypeMod(Material.STONE_SHOVEL));
        level.upgrade(new ModelDataMod(10040002));
      })

      .level(3, level -> {

      })

      .level(4, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 3));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.PIRATES_LUCK, 2));
      })

      .level(5, level -> {
        level.upgrade(new ItemTypeMod(Material.IRON_SHOVEL));
        level.upgrade(new ModelDataMod(10040003));
      })

      .level(6, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 4));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.PIRATES_LUCK, 3));
      })

      .level(7, level -> {
        level.upgrade(new ItemTypeMod(Material.DIAMOND_SHOVEL));
        level.upgrade(new ModelDataMod(10040004));
      })

      .level(8, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 5));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.PIRATES_LUCK, 4));
      })

      .level(9, level -> {
        level.upgrade(new ItemTypeMod(Material.NETHERITE_SHOVEL));
        level.upgrade(new ModelDataMod(10040005));
      })

      .level(10, level -> {
        level.upgrade(new AddEnchantMod(Enchantment.EFFICIENCY, 6));
        level.upgrade(new AddEnchantMod(ArcadiusEnchantments.PIRATES_LUCK, 5));
      })

      .build();

  public static final ItemGoals GOALS = ItemGoals.builder()
      .prefixedWith(LoreElement.BORDER)

      .level(1, level -> {
        level.add(Goal.blocksBroken(Material.GRASS_BLOCK, 1000));
      })

      .level(2, level -> {
        level
            .add(Goal.blocksBroken(Material.GRASS_BLOCK, 1_500))
            .add(pickupGoal(25, 1));
      })

      .level(3, level -> {
        level
            .add(Goal.blocksBroken(Material.SAND, 1000))
            .add(pickupGoal(50, 1))
            .add(pickupGoal(10, 10));
      })

      .level(4, level -> {
        level
            .add(Goal.blocksBroken(Material.SAND, 1500))
            .add(pickupGoal(75, 1))
            .add(pickupGoal(15, 10));
      })

      .level(5, level -> {
        level
            .add(Goal.blocksBroken(Material.DIRT, 1000))
            .add(Goal.blocksBroken(Material.SOUL_SAND, 1000))
            .add(pickupGoal(100, 1))
            .add(pickupGoal(20, 10))
            .add(pickupGoal(1, 100));
      })

      .level(6, level -> {
        level
            .add(Goal.blocksBroken(Material.DIRT, 1500))
            .add(Goal.blocksBroken(Material.SOUL_SAND, 1000))
            .add(pickupGoal(125, 1))
            .add(pickupGoal(25, 10))
            .add(pickupGoal(2, 100))
            .add(Goal.DONATOR);
      })

      .level(7, level -> {
        level
            .add(Goal.blocksBroken(Material.MUD, 1000))
            .add(Goal.blocksBroken(Material.MYCELIUM, 1000))
            .add(pickupGoal(150, 1))
            .add(pickupGoal(30, 10))
            .add(pickupGoal(3, 100));
      })

      .level(8, level -> {
        level
            .add(Goal.blocksBroken(Material.MUD, 1500))
            .add(Goal.blocksBroken(Material.MYCELIUM, 1500))
            .add(pickupGoal(175, 1))
            .add(pickupGoal(35, 10))
            .add(pickupGoal(4, 100));
      })

      .level(9, level -> {
        level
            .add(Goal.blocksBroken(Material.GRASS_BLOCK, 500))
            .add(Goal.blocksBroken(Material.SAND, 500))
            .add(Goal.blocksBroken(Material.SOUL_SAND, 500))
            .add(Goal.blocksBroken(Material.DIRT, 500))
            .add(Goal.blocksBroken(Material.MUD, 500))
            .add(Goal.blocksBroken(Material.MYCELIUM, 500))
            .add(Goal.blocksBroken(Material.CLAY, 500))
            .add(pickupGoal(200, 1))
            .add(pickupGoal(40, 10))
            .add(pickupGoal(5, 100));
      })

      .build();

  @Override
  public ItemStack createBaseItem() {
    ItemStack item = ItemStacks.builder(Material.WOODEN_SHOVEL)
        .setUnbreakable(true)
        .addFlags(ItemFlag.HIDE_ENCHANTS)
        .build();

    item.editMeta(meta -> {
      meta.itemName(Messages.renderText("itemsPlugin.spade.name"));
    });

    return item;
  }

  @Override
  public void addComponents(ExtendedItem item) {
    Level level = new Level(MAX_LEVEL);
    Owner owner = new Owner();

    item.addComponent(level);
    item.addComponent(owner);
    item.addComponent(UPGRADES.createComponent());
    item.addComponent(GOALS.createComponent());

    //item.addLore(EnchantsLoreElement.ENCHANTS);
    item.addLore(LoreElement.SINGLE_EMPTY_LINE);
    item.addLore(level);
    item.addLore(GOALS.createGoalText());
    item.addLore(UPGRADES.createPreviewElement());
    item.addLore(LoreElement.BORDER);
    item.addLore(CraftedForLore.ELEMENT);
    item.addLore(UPGRADES.createStatusElement());
  }

  private static Goal pickupGoal(int goal, int multiplier) {
    Component displayName = Messages.render("itemsPlugin.spade.pickupGoal")
        .addValue("multiplier", multiplier)
        .asComponent();

    return new Goal(GoalKey.valueOf(GOAL_TYPE, multiplier + "x"), displayName, goal);
  }

  @Override
  public boolean isPersistentBeyondDeath() {
    return true;
  }
}
