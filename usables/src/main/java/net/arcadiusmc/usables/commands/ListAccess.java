package net.arcadiusmc.usables.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.forthecrown.grenadier.CommandSource;
import net.arcadiusmc.usables.UsableComponent;

public interface ListAccess<T extends UsableComponent> {

  ListHolder<T> getHolder(CommandContext<CommandSource> context)
      throws CommandSyntaxException;
}
