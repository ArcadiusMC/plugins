package net.arcadiusmc.usables.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Commands;
import net.forthecrown.grenadier.CommandSource;
import net.arcadiusmc.usables.UPermissions;
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.usables.objects.UsableItem;

public class UsableItemCommand extends InWorldUsableCommand<UsableItem> {

  public UsableItemCommand() {
    super("usableitem", "item");
    setAliases("usable_item");
    setPermission(UPermissions.ITEM);
  }

  @Override
  public String getAdminPermission() {
    return UPermissions.ITEM.getName();
  }

  @Override
  protected ArgumentType<?> getArgumentType() {
    return null;
  }

  @Override
  protected UsableProvider<UsableItem> getProvider(String argument) {
    return new UsableProvider<>() {
      @Override
      public UsableItem get(CommandContext<CommandSource> context) throws CommandSyntaxException {
        var player = context.getSource().asPlayer();
        var held = Commands.getHeldItem(player);

        var usable = Usables.item(held);

        if (Usables.isUsable(held)) {
          usable.load();
        }

        return usable;
      }

      @Override
      public void postEdit(UsableItem holder) {
        holder.save();
      }
    };
  }
}
