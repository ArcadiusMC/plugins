package net.arcadiusmc.usables.items;

import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

  @Override
  public Component failMessage(Interaction interaction) {
    TextWriter writer = TextWriters.newWriter();

    boolean written = consumeInteraction(interaction, (player, itemStacks) -> {
      PlayerInventory inventory = player.getInventory();

      if (required) {
        writer.line("You need the following items:", NamedTextColor.GRAY);
      } else {
        writer.line("You must &lnot&r have the following items:", NamedTextColor.GRAY);
      }

      for (ItemStack itemStack : itemStacks) {
        boolean contains = inventory.containsAtLeast(itemStack, itemStack.getAmount());

        if (contains) {
          writer.formattedLine("- {0, item}", NamedTextColor.AQUA, itemStack);
        } else {
          writer.formattedLine("- {0, item}", NamedTextColor.GRAY, itemStack);
        }
      }

      return true;
    });

    if (!written) {
      return null;
    }

    return writer.asComponent();
  }
}
