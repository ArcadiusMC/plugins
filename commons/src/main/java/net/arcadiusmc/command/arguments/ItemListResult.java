package net.arcadiusmc.command.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.forthecrown.grenadier.CommandSource;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.inventory.ItemStack;

public interface ItemListResult {

  ItemListResult INVENTORY = source -> {
    var player = source.asPlayer();
    ItemList list = ItemLists.fromInventory(player.getInventory(), ItemStacks::notEmpty);

    if (list.isEmpty()) {
      throw Exceptions.create("Your inventory is empty");
    }

    return list;
  };

  ItemListResult HELD_ITEM = source -> {
    ItemStack held = Commands.getHeldItem(source.asPlayer());
    return ItemLists.newList(held);
  };

  ItemList get(CommandSource source) throws CommandSyntaxException;
}
