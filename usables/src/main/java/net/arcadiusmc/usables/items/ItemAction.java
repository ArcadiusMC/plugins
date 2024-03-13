package net.arcadiusmc.usables.items;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.slf4j.Logger;

public class ItemAction extends ItemComponent implements Action {

  private static final Logger LOGGER = Loggers.getLogger();

  private final boolean give;

  ItemAction(ItemProvider provider, boolean give) {
    super(provider);
    this.give = give;
  }

  @Override
  public void onUse(Interaction interaction) {
    consumeInteraction(interaction, (player, list) -> {
      PlayerInventory inventory = player.getInventory();

      if (give) {
        ItemStacks.giveOrDrop(inventory, list);
        return true;
      }

      ItemStack[] items = list.toItemArray();
      inventory.removeItemAnySlot(items);

      return true;
    });
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return give ? ItemActionType.GIVE : ItemActionType.TAKE;
  }
}
