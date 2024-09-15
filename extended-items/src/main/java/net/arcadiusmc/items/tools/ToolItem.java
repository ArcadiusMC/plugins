package net.arcadiusmc.items.tools;

import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.Owner;
import net.arcadiusmc.items.goal.ItemGoals;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.ItemUpgrades;

class ToolItem {

  static void configure(ExtendedItem item, int maxLevel, ItemUpgrades upgrades, ItemGoals goals) {
    Level level = new Level(maxLevel);
    Owner owner = new Owner();

    item.addComponent(level);
    item.addComponent(owner);
    item.addComponent(upgrades.createComponent());
    item.addComponent(goals.createComponent());

    item.addLore(LoreElement.SINGLE_EMPTY_LINE);
    item.addLore(level);
    item.addLore(goals.createGoalText());
    item.addLore(upgrades.createPreviewElement());
    item.addLore(LoreElement.BORDER);
    item.addLore(CraftedForLore.ELEMENT);
    item.addLore(upgrades.createStatusElement());
  }
}
