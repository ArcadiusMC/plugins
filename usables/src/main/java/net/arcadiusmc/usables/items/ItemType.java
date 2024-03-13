package net.arcadiusmc.usables.items;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.usables.ObjectType;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Readers;
import org.jetbrains.annotations.NotNull;

public abstract class ItemType<T extends ItemComponent> implements ObjectType<T> {

  abstract T construct(ItemProvider provider);

  @Override
  public T parse(StringReader reader, CommandSource source) throws CommandSyntaxException {
    ProviderParser<CommandSource> parser = new ProviderParser<>(reader, source);
    ItemProvider provider = parser.parse();

    return construct(provider);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    StringReader reader = Readers.forSuggestions(builder);
    ProviderParser<CommandSource> parser = new ProviderParser<>(reader, context.getSource());

    try {
      parser.parse();
    } catch (CommandSyntaxException ignored) {
      // Ignored :)
    }

    return parser.getSuggestions(context, builder);
  }

  @Override
  public <S> DataResult<T> load(Dynamic<S> dynamic) {
    return ItemProvider.CODEC.parse(dynamic).map(this::construct);
  }

  @Override
  public <S> DataResult<S> save(@NotNull T value, @NotNull DynamicOps<S> ops) {
    return ItemProvider.CODEC.encodeStart(ops, value.getProvider());
  }

}
