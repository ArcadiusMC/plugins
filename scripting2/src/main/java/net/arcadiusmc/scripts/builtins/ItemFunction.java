package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.CommandSource;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public enum ItemFunction implements ProxyExecutable {
  GIVE,
  TAKE,
  ;

  @Override
  public Object execute(Value... arguments) {
    Scripts.ensureParameterCount(arguments, 2);;

    InventoryHolder holder = toInventoryHolder(arguments, 0);

    if (holder == null) {
      throw Scripts.typeError("First argument must be inventory-holder / inventory");
    }

    Inventory inventory = holder.getInventory();
    ItemStack item = Scripts.toItemStack(arguments[1]);

    if (this == GIVE) {
      ItemStacks.giveOrDrop(inventory, item);
    } else {
      inventory.removeItemAnySlot(item);
    }

    return null;
  }


  private InventoryHolder toInventoryHolder(Value[] args, int index) {
    if (args.length <= index) {
      return null;
    }

    try {
      CommandSource source = Scripts.toSource(args, index);
      if (source.asBukkit() instanceof InventoryHolder holder) {
        return holder;
      }
    } catch (RuntimeException exc) {
      // Ignored, means the value is not a command source
    }

    Value value = args[index];
    if (!value.isHostObject()) {
      return null;
    }

    Object hostValue = value.asHostObject();

    if (hostValue instanceof Block block) {
      BlockState state = block.getState();

      if (state instanceof InventoryHolder holder) {
        return holder;
      }

      return null;
    }

    if (hostValue instanceof InventoryHolder holder) {
      return holder;
    }

    if (hostValue instanceof Inventory inventory) {
      if (inventory.getHolder() == null) {
        return () -> inventory;
      }
      return inventory.getHolder();
    }

    return null;
  }
}
