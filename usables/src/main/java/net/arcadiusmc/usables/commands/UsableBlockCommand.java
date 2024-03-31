package net.arcadiusmc.usables.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Exceptions;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.arcadiusmc.usables.UPermissions;
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.usables.objects.UsableBlock;
import net.arcadiusmc.utils.math.Vectors;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;

public class UsableBlockCommand extends InWorldUsableCommand<UsableBlock> {

  public UsableBlockCommand() {
    super("usableblock");
    setAliases("usable_block", "usable-block");
    setPermission(UPermissions.BLOCK);
  }

  @Override
  public String getAdminPermission() {
    return UPermissions.BLOCK.getName();
  }

  @Override
  protected String usagePrefix() {
    return "<block: x,y,z>";
  }

  @Override
  protected ArgumentType<?> getArgumentType() {
    return ArgumentTypes.blockPosition();
  }

  @Override
  protected UsableProvider<UsableBlock> getProvider(String argument) {
    return new UsableProvider<>() {
      @Override
      public UsableBlock get(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ParsedPosition position = context.getArgument(argument, ParsedPosition.class);
        Location loc = position.apply(context.getSource());
        Block block = loc.getBlock();

        if (!(block.getState() instanceof TileState)) {
          throw Exceptions.format("Block {0, vector} is not a block entity", Vectors.from(block));
        }

        var usable = Usables.block(block);

        if (Usables.isUsable(block)) {
          usable.load();
        }

        return usable;
      }

      @Override
      public void postEdit(UsableBlock holder) {
        holder.save();
      }
    };
  }
}
