package net.arcadiusmc.factions.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionManager;
import net.arcadiusmc.factions.Factions;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.Completions;

public enum FactionArgument implements ArgumentType<Faction> {
  FACTION;

  @Override
  public Faction parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();
    String resourceKey = Arguments.RESOURCE_KEY.parse(reader);

    FactionManager manager = Factions.getManager();
    Faction found = manager.getFaction(resourceKey);

    if (found != null) {
      return found;
    }

    reader.setCursor(start);

    throw Messages.render("factions.errors.unknown")
        .addValue("label", resourceKey)
        .exceptionWithContext(reader);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    FactionManager manager = Factions.getManager();
    String token = builder.getRemainingLowerCase();

    for (Faction faction : manager.getFactions()) {
      if (!Completions.matches(token, faction.getKey())) {
        continue;
      }

      builder.suggest(faction.getKey());
    }

    return builder.buildFuture();
  }
}
