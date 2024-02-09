package net.arcadiusmc.leaderboards.commands;

import com.mojang.serialization.DataResult;
import net.arcadiusmc.command.Commands;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.arcadiusmc.leaderboards.LeaderboardPlugin;
import net.arcadiusmc.leaderboards.ScoreFilter;
import net.arcadiusmc.utils.io.FtcCodecs;

public final class LeaderboardCommands {
  private LeaderboardCommands() {}

  public static final FilterArgument FILTER = new FilterArgument();

  public static DataResult<ScoreFilter> parseFilter(String s) {
    return FtcCodecs.safeParse(s, FILTER);
  }

  public static void createCommands(LeaderboardPlugin plugin) {
    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandLeaderboard(plugin));
  }
}
