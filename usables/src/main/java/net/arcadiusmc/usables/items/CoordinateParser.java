package net.arcadiusmc.usables.items;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.CoordinateSuggestion;
import net.forthecrown.grenadier.types.ParsedPosition.Coordinate;

enum CoordinateParser implements ArgumentType<Coordinate> {
  X, Y, Z;

  @Override
  public Coordinate parse(StringReader reader) throws CommandSyntaxException {
    boolean relative;

    if (reader.peek() == '~') {
      relative = true;
      reader.skip();
    } else {
      relative = false;
    }

    if (!reader.canRead() || Character.isWhitespace(reader.peek())) {
      return new Coordinate(0, relative);
    }

    double value = reader.readDouble();
    return new Coordinate(value, relative);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    if (!(context.getSource() instanceof CommandSource source)) {
      return Suggestions.empty();
    }

    CoordinateSuggestion suggestion = source.getRelevant3DCords();

    if (suggestion == null) {
      return Suggestions.empty();
    }

    String coordinate = switch (this) {
      case X -> suggestion.x();
      case Y -> suggestion.y();
      case Z -> suggestion.z();
    };

    return Completions.suggest(builder, coordinate);
  }
}
