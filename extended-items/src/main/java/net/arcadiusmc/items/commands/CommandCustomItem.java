package net.arcadiusmc.items.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.items.ItemTypes;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.entity.Player;

public class CommandCustomItem extends BaseCommand {

  private final RegistryArguments<ItemType> itemTypeArgument;

  public CommandCustomItem() {
    super("customitems");

    itemTypeArgument = new RegistryArguments<>(ItemTypes.REGISTRY, "Item Type");

    setAliases("custom-items", "ci");
    setDescription("Custom items command");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("give")
            .then(argument("item type", itemTypeArgument)
                .executes(c -> {
                  give(c, false);
                  return 0;
                })

                .then(argument("owner", Arguments.USER)
                    .executes(c -> {
                      give(c, true);
                      return 0;
                    })
                )
            )
        );
  }

  private void give(CommandContext<CommandSource> c, boolean ownerSet)
      throws CommandSyntaxException
  {
    UUID ownerId;

    if (ownerSet) {
      User owner = Arguments.getUser(c, "owner");
      ownerId = owner.getUniqueId();
    } else {
      ownerId = null;
    }

    Holder<ItemType> holder = c.getArgument("item type", Holder.class);
    ExtendedItem item = ItemTypes.createItem(holder, ownerId);

    Player player = c.getSource().asPlayer();
    ItemStacks.giveOrDrop(player.getInventory(), item.getHandle());

    c.getSource().sendSuccess(
        Messages.render("itemsPlugin.cmd.give")
            .addValue("item", Text.itemDisplayName(item.getHandle()))
            .create(c.getSource())
    );
  }
}
