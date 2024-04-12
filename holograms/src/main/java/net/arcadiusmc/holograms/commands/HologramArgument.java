package net.arcadiusmc.holograms.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.holograms.ServiceImpl;
import net.arcadiusmc.holograms.TextImpl;
import net.forthecrown.grenadier.Completions;

public class HologramArgument implements ArgumentType<TextImpl> {

  private final ServiceImpl service;

  public HologramArgument(ServiceImpl service) {
    this.service = service;
  }

  @Override
  public TextImpl parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();
    String key = Arguments.RESOURCE_KEY.parse(reader);
    Optional<TextImpl> opt = service.getHologram(key)
        .map(textHologram -> (TextImpl) textHologram);

    if (opt.isEmpty()) {
      reader.setCursor(start);
      throw Exceptions.unknown("Hologram", reader, key);
    }

    return opt.get();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return Completions.suggest(builder, service.getExistingHolograms());
  }
}
