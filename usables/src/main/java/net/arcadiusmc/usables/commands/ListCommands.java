package net.arcadiusmc.usables.commands;

import static net.forthecrown.grenadier.Nodes.argument;
import static net.forthecrown.grenadier.Nodes.literal;
import static net.kyori.adventure.text.Component.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import lombok.Getter;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.usables.Condition.TransientCondition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.list.ComponentList;
import net.forthecrown.grenadier.CommandContexts;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

public class ListCommands<T extends UsableComponent> {

  static final int POS_FIRST  = 0;
  static final int POS_LAST   = -1;
  static final int POS_AT     = 1;

  private final Registry<ObjectType<T>> registry;

  @Getter
  private final RegistryArguments<ObjectType<T>> argument;

  private final String displayName;
  private final String argumentName;

  public ListCommands(
      String argumentName,
      String displayName,
      Registry<ObjectType<? extends T>> registry
  ) {
    this.argumentName = argumentName;
    this.displayName = displayName;
    this.registry = (Registry) registry;

    this.argument = new RegistryArguments<>(this.registry, displayName);
  }

  public void createUsages(UsageFactory factory) {
    createUsagesNoPrefix(factory.withPrefix(argumentName));
  }

  public void createUsagesNoPrefix(UsageFactory factory) {
    factory.usage("").addInfo("Shows all the existing %ss", displayName);
    factory.usage("clear").addInfo("Clears the %s list", displayName);

    factory.usage("remove <indices>")
        .addInfo("Removes a %s", displayName)
        .addInfo("Examples:")
        .addInfo("- remove 1: Removes an element at index 1")
        .addInfo("- remove 1..3: Removes all elements between indices 1 through 3");

    factory.usage("add <type> [<input>]")
        .addInfo("Adds a %s", displayName);

    factory.usage("add -at <index> <type> [<input>]")
        .addInfo("Adds an %s before the element at <index>", displayName);

    factory.usage("add -first <type> [<input>]")
        .addInfo("Adds a %s to the front of the %ss list", displayName, displayName);

    factory.usage("set <index> <type> [<input>]")
        .addInfo("Sets the %s at <index> to <type>", displayName);
  }

  public LiteralArgumentBuilder<CommandSource> create(ListAccess<T> access) {
    var literal = literal(argumentName);
    addArguments(access, literal);
    return literal;
  }

  private Component show(ListHolder<T> holder) {
    ComponentList<T> list = holder.getList();

    var writer = TextWriters.newWriter();
    writer.setFieldStyle(Style.style(NamedTextColor.GRAY));

    var name = holder.object().displayName();

    writer.field(name.append(text(" " + displayName + "s")));
    list.write(writer, holder.object().getCommandPrefix() + " " + argumentName);

    writer.newLine();

    return writer.asComponent();
  }

  public void addArguments(
      ListAccess<T> access,
      ArgumentBuilder<CommandSource, ? extends ArgumentBuilder> builder
  ) {
    builder.executes(context -> {
      ListHolder<T> holder = access.getHolder(context);
      context.getSource().sendMessage(show(holder));
      return 0;
    });

    builder.then(literal("add")
        .then(literal("at")
            .then(argument("index", IntegerArgumentType.integer(1))
                .suggests(suggestIndices(access))
                .then(addArgument(access, POS_AT))
            )
        )

        .then(literal("first").then(addArgument(access, POS_FIRST)))
        .then(addArgument(access, POS_LAST))
    );

    builder.then(literal("remove").then(argument("range", ArgumentTypes.intRange())
        .suggests(suggestIndices(access))

        .executes(c -> {
          var holder = access.getHolder(c);
          var list = holder.getList();

          if (list.isEmpty()) {
            throw Exceptions.format("No {0}s to remove", displayName);
          }

          IntRange range = c.getArgument("range", IntRange.class);

          int minIndex = range.min().orElse(1);
          int maxIndex = range.max().orElse(list.size());

          Commands.ensureIndexValid(minIndex, list.size());
          Commands.ensureIndexValid(maxIndex, list.size());

          Component message;

          if (minIndex == maxIndex) {
            // Ensure hardcoded condition is not removed
            T value = list.get(minIndex - 1);
            if (value instanceof TransientCondition) {
              throw Exceptions.create("Cannot remove hardcoded condition");
            }

            list.remove(minIndex - 1);

            message = Text.format("Removed {0} at index &f{1, number}&r.",
                NamedTextColor.GRAY,
                displayName, minIndex
            );
          } else {
            // Ensure hardcoded condition is not removed
            for (int i = minIndex -1; i < maxIndex; i++) {
              T value = list.get(minIndex - 1);

              if (value instanceof TransientCondition) {
                throw Exceptions.format("Cannot remove hardcoded condition at index {0, number}",
                    i + 1
                );
              }
            }

            list.removeBetween(minIndex - 1, maxIndex);

            message = Text.format(
                "Removed all {0}s between &f{1, number}&r and &f{2, number}&r.",
                NamedTextColor.GRAY,
                displayName, minIndex, maxIndex
            );
          }

          holder.postEdit();

          c.getSource().sendSuccess(message);
          c.getSource().sendMessage(show(holder));

          return 0;
        })
    ));

    builder.then(literal("set").then(argument("index", IntegerArgumentType.integer(1))
        .suggests(suggestIndices(access))

        .then(argument("type", argument)
            .executes(c -> {
              StringReader reader = new StringReader(c.getInput());
              reader.setCursor(reader.getTotalLength());

              doSet(c, access, reader);
              return 0;
            })

            .then(argument("input", StringArgumentType.greedyString())
                .suggests((context, builder1) -> {
                  Holder<ObjectType<T>> holder = context.getArgument("type", Holder.class);
                  return holder.getValue().getSuggestions(context, builder1);
                })

                .executes(c -> {
                  StringRange range = CommandContexts.getNodeRange(c, "input");
                  StringReader reader = new StringReader(c.getInput());
                  reader.setCursor(range.getStart());

                  doSet(c, access, reader);
                  return 0;
                })
            )
        )
    ));

    builder.then(literal("clear").executes(c -> {
      var holder = access.getHolder(c);
      var list = holder.getList();

      if (list.isEmpty()) {
        throw Exceptions.create(displayName + " list already empty");
      }

      list.clear();
      holder.postEdit();

      c.getSource().sendSuccess(
          Text.format("Cleared {0}s list for &e{1}&r.",
              NamedTextColor.GRAY,
              displayName,
              holder.object().displayName()
          )
      );
      return 0;
    }));
  }

  public SuggestionProvider<CommandSource> suggestIndices(ListAccess<T> access) {
    return (context, builder) -> {
      var holder = access.getHolder(context);
      var list = holder.getList();

      if (list.isEmpty()) {
        return Suggestions.empty();
      }

      var input = builder.getRemainingLowerCase();

      for (int i = 0; i < list.size(); i++) {
        String suggestion = String.valueOf(i + 1);

        if (!Completions.matches(input, suggestion)) {
          continue;
        }

        Component hover = list.displayEntry(i, holder.object().getCommandPrefix());
        builder.suggest(suggestion, Grenadier.toMessage(hover));
      }

      return builder.buildFuture();
    };
  }

  private RequiredArgumentBuilder<CommandSource, ?> addArgument(
      ListAccess<T> access,
      int positioned
  ) {
    return argument("type", argument)

        .suggests((context, builder) -> {
          var entries = registry.entries();
          var holder = access.getHolder(context);

          return Completions.suggest(builder,
              entries.stream()
                  .filter(typeHolder -> {
                    return typeHolder.getValue().canApplyTo(holder.object());
                  })

                  .map(Holder::getKey)
          );
        })

        .executes(c -> {
          StringReader reader = new StringReader(c.getInput());
          reader.setCursor(reader.getTotalLength());
          doAdd(c, access, positioned, reader);
          return 0;
        })

        .then(argument("input", StringArgumentType.greedyString())
            .suggests((context, builder1) -> {
              Holder<ObjectType<T>> holder = context.getArgument("type", Holder.class);
              return holder.getValue().getSuggestions(context, builder1);
            })

            .executes(c -> {
              StringRange range = CommandContexts.getNodeRange(c, "input");
              StringReader reader = new StringReader(c.getInput());
              reader.setCursor(range.getStart());

              doAdd(c, access, positioned, reader);
              return 0;
            })
        );
  }

  private void doSet(
      CommandContext<CommandSource> c,
      ListAccess<T> access,
      StringReader reader
  ) throws CommandSyntaxException {
    ListHolder<T> listHolder = access.getHolder(c);
    ComponentList<T> list = listHolder.getList();

    Holder<ObjectType<T>> holder = c.getArgument("type", Holder.class);
    ObjectType<T> type = holder.getValue();

    validateApplicable(holder, listHolder);

    T instance = type.parse(reader, c.getSource());

    if (reader.canRead()) {
      Commands.ensureCannotRead(reader);
    }

    int index = c.getArgument("index", Integer.class);
    Commands.ensureIndexValid(index, list.size());

    list.set(index - 1, instance);
    listHolder.postEdit();

    CommandSource source = c.getSource();
    Component displayInfo = instance.displayInfo();

    if (displayInfo == null) {
      source.sendSuccess(Text.format("Set {0} at index &e{1, number}&r to &6{2}&r.",
          NamedTextColor.GRAY,
          displayName, index, holder.getKey()
      ));
    } else {
      c.getSource().sendSuccess(Text.format("Set {0} at index &e{1, number}&r to &6{2}&r: &f",
          NamedTextColor.GRAY,
          displayName, index, holder.getKey(), displayInfo
      ));
    }
  }

  private void validateApplicable(Holder<ObjectType<T>> holder, ListHolder<T> listHolder)
      throws CommandSyntaxException
  {
    if (!holder.getValue().canApplyTo(listHolder.object())) {
      throw Exceptions.format("Type {0} cannot be applied to {1}",
          holder.getKey(),
          listHolder.object().displayName()
      );
    }
  }

  private void doAdd(
      CommandContext<CommandSource> c,
      ListAccess<T> access,
      int positioned,
      StringReader reader
  ) throws CommandSyntaxException {
    ListHolder<T> listHolder = access.getHolder(c);
    ComponentList<T> list = listHolder.getList();

    Holder<ObjectType<T>> holder = c.getArgument("type", Holder.class);
    ObjectType<T> type = holder.getValue();

    validateApplicable(holder, listHolder);

    T instance = type.parse(reader, c.getSource());

    if (reader.canRead()) {
      Commands.ensureCannotRead(reader);
    }

    int index = switch (positioned) {
      case POS_LAST -> list.size();
      case POS_FIRST -> 0;
      case POS_AT -> {
        int i = c.getArgument("index", Integer.class);
        Commands.ensureIndexValid(i, list.size() + 1);
        yield i - 1;
      }

      default -> positioned;
    };

    list.add(instance, index);
    listHolder.postEdit();

    Component displayInfo = instance.displayInfo();
    CommandSource source = c.getSource();

    if (displayInfo == null) {
      source.sendSuccess(Text.format("Added {0} &e{1}&r.",
          NamedTextColor.GRAY,
          displayName, holder.getKey()
      ));
    } else {
      source.sendSuccess(Text.format("Added {0} &e{1}&r: &f{2}",
          NamedTextColor.GRAY,
          displayName, holder.getKey(), displayInfo
      ));
    }

    source.sendMessage(show(listHolder));
  }
}
