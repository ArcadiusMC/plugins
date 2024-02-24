package net.arcadiusmc.holograms.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.forthecrown.grenadier.Completions;
import net.arcadiusmc.holograms.BoardImpl;
import net.arcadiusmc.holograms.ServiceImpl;

public class LeaderboardArgument implements ArgumentType<BoardImpl> {

  private final ServiceImpl service;

  public LeaderboardArgument(ServiceImpl service) {
    this.service = service;
  }

  @Override
  public BoardImpl parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();

    String ftcKey = Arguments.RESOURCE_KEY.parse(reader);
    var opt = service.getBoard(ftcKey);

    if (opt.isEmpty()) {
      reader.setCursor(start);
      throw Exceptions.unknown("Leaderboard", reader, ftcKey);
    }

    return opt.get();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return Completions.suggest(builder, service.getExistingLeaderboards());
  }
}
