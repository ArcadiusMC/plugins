package net.arcadiusmc.holograms.commands;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.holograms.HologramPlugin;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;

public final class LeaderboardCommands {
  private LeaderboardCommands() {}

  public static void createCommands(HologramPlugin plugin) {
    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandLeaderboard(plugin));
  }
}
