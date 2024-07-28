package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Iterator;
import java.util.Map.Entry;
import net.arcadiusmc.command.DataCommands;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.string.Snbt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

public class ItemDataNode extends ItemModifierNode {

  public ItemDataNode() {
    super(
        "item_data",
        "item_tags", "itemnbt", "itemdata", "itemtags"
    );
  }

  @Override
  String getArgumentName() {
    return "data";
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("give-command", "Creates a `/give` command for the item you're holding");

    DataCommands.addUsages(factory, "Item", usage -> {
      var label = usage.getArguments();

      if (!label.contains("<tag") && !label.contains("<path")) {
        return;
      }

      usage.addInfo("Note: the <tag> and <path> roots are the item's raw NBT")
          .addInfo("Not the 'tag' element.");
    });
  }

  @Override
  public void create(LiteralArgumentBuilder<CommandSource> command) {
    DataCommands.addArguments(
        command,
        "Item",
        DataCommands.HELD_ITEM_ACCESSOR
    );

    command
        .then(literal("give-command")
            .executes(c -> {
              CommandSource source = c.getSource();
              ItemStack held = getHeld(source);

              CompoundTag tag = ItemStacks.save(held);
              CompoundTag components = tag.getCompound("components");

              StringBuilder builder = new StringBuilder();
              builder.append("/give @s ")
                  .append(held.getType().key());

              if (!components.isEmpty()) {
                Iterator<Entry<String, BinaryTag>> it = components.entrySet().iterator();
                builder.append('[');

                while (it.hasNext()) {
                  Entry<String, BinaryTag> e = it.next();
                  builder.append(e.getKey());
                  builder.append('=');
                  builder.append(Snbt.toString(e.getValue(), false, false));

                  if (it.hasNext()) {
                    builder.append(",");
                  }
                }

                builder.append(']');
              }

              int amount = held.getAmount();
              if (amount > 1) {
                builder.append(' ').append(amount);
              }

              String commandString = builder.toString();

              source.sendMessage(
                  Component.text("[Click to copy /give command]", NamedTextColor.AQUA)
                      .clickEvent(ClickEvent.copyToClipboard(commandString))
                      .hoverEvent(Component.text(commandString))
              );

              return 0;
            })
        );

  }
}