package net.arcadiusmc.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.internal.SimpleVanillaMapped;

@Getter
@SuppressWarnings({"unchecked", "rawtypes"})
public class RegistryArguments<T> implements ArgumentType<Holder<T>>, SimpleVanillaMapped {

  private final Registry<T> registry;
  private final String unknown;

  public RegistryArguments(Registry<T> registry, String unknownMessage) {
    this.registry = registry;
    this.unknown = unknownMessage;
  }

  @Override
  public Holder<T> parse(StringReader reader) throws CommandSyntaxException {
    int cursor = reader.getCursor();
    String key = Arguments.RESOURCE_KEY.parse(reader);

    return registry.getHolder(key).orElseThrow(() -> {
      reader.setCursor(cursor);
      return Exceptions.unknown(unknown, reader, key);
    });
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    return Completions.suggest(builder, registry.keys());
  }

  @Override
  public ArgumentType<?> getVanillaType() {
    return Arguments.RESOURCE_KEY.getVanillaType();
  }
}