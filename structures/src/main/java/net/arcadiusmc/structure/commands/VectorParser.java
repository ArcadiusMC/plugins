package net.arcadiusmc.structure.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.forthecrown.grenadier.Completions;
import org.spongepowered.math.vector.Vector3d;

public class VectorParser implements ArgumentType<Vector3d> {

  @Override
  public Vector3d parse(StringReader reader) throws CommandSyntaxException {
    double x = reader.readDouble();
    skipSeparator(reader);
    double y = reader.readDouble();
    skipSeparator(reader);
    double z = reader.readDouble();

    return Vector3d.from(x, y, z);
  }

  private void skipSeparator(StringReader reader) {
    reader.skipWhitespace();

    if (reader.canRead() && reader.peek() == ',') {
      reader.skip();
      reader.skipWhitespace();
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return Completions.suggest(builder, "0 0 0", "1 1 1", "-1 -1 -1");
  }
}
