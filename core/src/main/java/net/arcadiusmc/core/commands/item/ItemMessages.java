package net.arcadiusmc.core.commands.item;

import static net.arcadiusmc.text.Messages.MESSAGE_LIST;

import net.arcadiusmc.text.loader.MessageRef;

interface ItemMessages {

  // Itemname
  MessageRef NAME_CLEARED = MESSAGE_LIST.reference("cmd.itemname.cleared");
  MessageRef NAME_SET = MESSAGE_LIST.reference("cmd.itemname.set");

  // Lore
  MessageRef NO_LORE = MESSAGE_LIST.reference("cmd.lore.error.noLore");
  MessageRef ADDED_LORE = MESSAGE_LIST.reference("cmd.lore.added");
  MessageRef REMOVED_LORE_RANGE = MESSAGE_LIST.reference("cmd.lore.removed.range");
  MessageRef REMOVED_LORE_INDEX = MESSAGE_LIST.reference("cmd.lore.removed.index");
  MessageRef SET_LORE = MESSAGE_LIST.reference("cmd.lore.set");
  MessageRef LORE_DISPLAY = MESSAGE_LIST.reference("cmd.lore.display");
  MessageRef LORE_DISPLAY_LINE = MESSAGE_LIST.reference("cmd.lore.display.line");
  MessageRef LORE_CLEARED = MESSAGE_LIST.reference("cmd.lore.cleared");

  // Enchants
  MessageRef CLEARED_ENCHANTMENTS = MESSAGE_LIST.reference("cmd.enchant.cleared");
  MessageRef ADDED_ENCHANTMENT = MESSAGE_LIST.reference("cmd.enchant.added");
  MessageRef REMOVED_ENCHANTMENT = MESSAGE_LIST.reference("cmd.enchant.removed");
  MessageRef ENCHANT_LEVEL_TOO_LOW = MESSAGE_LIST.reference("cmd.enchant.error.levelTooLow");

  // Item cooldown
  MessageRef SET_COOLDOWN = MESSAGE_LIST.reference("cmd.itemcooldown.set");
  MessageRef REMOVED_COOLDOWN = MESSAGE_LIST.reference("cmd.itemcooldown.removed");
  MessageRef NO_COOLDOWN = MESSAGE_LIST.reference("cmd.itemcooldown.error.noCooldown");
  MessageRef COOLDOWN_DISPLAY = MESSAGE_LIST.reference("cmd.itemcooldown.display");
}
