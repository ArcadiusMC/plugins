package net.arcadiusmc.usables.scripts;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.scripts.commands.ScriptArgument;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.utils.io.source.Source;
import net.arcadiusmc.utils.io.source.Sources;
import net.forthecrown.grenadier.CommandSource;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScriptType implements ObjectType<ScriptInstance> {

  private final boolean rawJs;

  public ScriptType(boolean rawJs) {
    this.rawJs = rawJs;
  }

  @Override
  public ScriptInstance parse(StringReader reader, CommandSource cmdSource)
      throws CommandSyntaxException
  {
    Source source;
    String[] args = ArrayUtils.EMPTY_STRING_ARRAY;

    if (rawJs) {
      String remaining = reader.getRemaining();
      reader.setCursor(reader.getTotalLength());
      source = Sources.direct(remaining, "<raw-js, by=" + cmdSource.textName() + ", js=" + remaining + ">");
    } else {
      source = ScriptArgument.SCRIPT.parse(reader);
      reader.skipWhitespace();

      if (reader.canRead()) {
        String remaining = reader.getRemaining().trim();
        reader.setCursor(reader.getTotalLength());
        args = remaining.split(" ");
      }
    }

    return new ScriptInstance(source, args);
  }

  @Override
  public @NotNull <S> DataResult<ScriptInstance> load(@Nullable Dynamic<S> dynamic) {
    return dynamic.get("script")
        .flatMap(dynamic1 -> Scripts.loadScriptSource(dynamic1, rawJs))
        .map(source1 -> {
          String[] args = dynamic.get("args")
              .asList(dynamic1 -> dynamic1.asString(""))
              .toArray(String[]::new);

          return new ScriptInstance(source1, args);
        });
  }

  @Override
  public <S> DataResult<S> save(@NotNull ScriptInstance value, @NotNull DynamicOps<S> ops) {
    RecordBuilder<S> mapBuilder = ops.mapBuilder();
    mapBuilder.add("script", value.getSource().save(ops));

    var args = ops.createList(Arrays.stream(value.getArgs()).map(ops::createString));
    mapBuilder.add("args", args);

    return mapBuilder.build(ops.empty());
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    if (rawJs) {
      return Suggestions.empty();
    }

    return ScriptArgument.SCRIPT.listSuggestions(context, builder);
  }
}
