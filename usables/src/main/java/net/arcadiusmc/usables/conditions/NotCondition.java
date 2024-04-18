package net.arcadiusmc.usables.conditions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.Getter;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.commands.UsablesCommands;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.Suggester;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NotCondition implements Condition {

  static final NotConditionType TYPE = new NotConditionType();

  @Getter
  private final Condition condition;

  public NotCondition(Condition condition) {
    this.condition = condition;
  }

  @Override
  public boolean test(Interaction interaction) {
    return !condition.test(interaction);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    Component message = condition.failMessage(interaction);

    if (Text.isEmpty(message)) {
      return null;
    }

    return Component.textOfChildren(Component.text("NOT ").style(message.style()), message);
  }

  @Override
  public void afterTests(Interaction interaction) {
    condition.afterTests(interaction);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    ObjectType<Condition> type = (ObjectType<Condition>) condition.getType();
    Component typeName;

    if (type == null) {
      typeName = Component.text("TRANSIENT");
    } else {
      String typeString = UsablesPlugin.get().getConditions().getKey(type).orElse("TRANSIENT");
      typeName = Component.text(typeString);
    }

    typeName = typeName.color(NamedTextColor.YELLOW);

    Component displayInfo = condition.displayInfo();

    if (displayInfo == null) {
      return typeName;
    }

    return Component.textOfChildren(typeName, Component.text(": "), displayInfo);
  }
}

class NotConditionType implements ObjectType<NotCondition> {

  static final Codec<ObjectType<? extends Condition>> TYPE_CODEC
      = UsablesPlugin.get().getConditions()
      .registryCodec()
      .fieldOf("type")
      .codec();

  @Override
  public NotCondition parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    NotParser parser = new NotParser(reader, source);
    parser.parse();
    return new NotCondition(parser.condition);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    StringReader reader = Readers.forSuggestions(builder);
    NotParser parser = new NotParser(reader, context.getSource());

    try {
      parser.parse();
    } catch (CommandSyntaxException exc) {
      // ignored
    }

    return parser.getSuggestions(context, builder);
  }

  @Override
  public <S> DataResult<NotCondition> load(Dynamic<S> dynamic) {
    DataResult<ObjectType<? extends Condition>> result = TYPE_CODEC.parse(dynamic);

    return result
        .apply2(
            (objectType, s) -> {
              return objectType.load(new Dynamic<>(dynamic.getOps(), s));
            },
            dynamic.getElement("value")
        )
        .flatMap(Function.identity())
        .map(NotCondition::new);
  }

  @Override
  public <S> DataResult<S> save(@NotNull NotCondition value, @NotNull DynamicOps<S> ops) {
    RecordBuilder<S> builder = ops.mapBuilder();

    ObjectType<Condition> type = (ObjectType<Condition>) value.getCondition().getType();
    DataResult<S> typeResult = UsablesPlugin.get().getConditions().encode(ops, type);

    builder.add("type", typeResult);
    builder.add("value", type.save(value.getCondition(), ops));

    return builder.build(ops.empty());
  }
}

class NotParser implements Suggester<CommandSource> {

  private final StringReader reader;
  private final CommandSource source;

  ObjectType<Condition> type;
  Condition condition;

  private Suggester<CommandSource> suggester;

  public NotParser(StringReader reader, CommandSource source) {
    this.reader = reader;
    this.source = source;
  }

  void parse() throws CommandSyntaxException {
    suggester = (context, builder) -> {
      return UsablesCommands.conditions.getArgument().listSuggestions(context, builder);
    };

    Holder<ObjectType<Condition>> holder = UsablesCommands.conditions.getArgument().parse(reader);
    type = holder.getValue();

    reader.skipWhitespace();

    int start = reader.getCursor();
    suggester = (context, builder) -> {
      builder = builder.createOffset(start);
      return type.getSuggestions(context, builder);
    };

    condition = type.parse(reader, source);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    if (suggester == null) {
      return UsablesCommands.conditions.getArgument().listSuggestions(context, builder);
    }

    return suggester.getSuggestions(context, builder);
  }
}