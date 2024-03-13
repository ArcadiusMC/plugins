package net.arcadiusmc.usables.items;

import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemTest extends ItemComponent implements Condition {

  private final boolean required;

  ItemTest(ItemProvider provider, boolean required) {
    super(provider);
    this.required = required;
  }

  @Override
  public boolean test(Interaction interaction) {
    return consumeInteraction(interaction, (player, list) -> {
      PlayerInventory inventory = player.getInventory();

      for (ItemStack itemStack : list) {
        boolean contains = inventory.containsAtLeast(itemStack, itemStack.getAmount());

        if (required != contains) {
          return false;
        }
      }

      return true;
    });
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return required ? ItemTestType.CONTAINS : ItemTestType.NOT;
  }
}
