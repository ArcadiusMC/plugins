package net.arcadiusmc.factions.commands;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.factions.FactionsPlugin;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;

public class FactionCommands {

  public static void createCommands(FactionsPlugin plugin) {
    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandFactions(plugin));
  }
}
