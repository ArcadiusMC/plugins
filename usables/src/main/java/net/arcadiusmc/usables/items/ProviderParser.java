package net.arcadiusmc.usables.items;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.usables.items.ItemProvider.ItemListProvider;
import net.arcadiusmc.usables.items.ItemProvider.PositionRefItemProvider;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.Suggester;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ArrayArgument;
import net.forthecrown.grenadier.types.ParsedPosition.Coordinate;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

class ProviderParser<S> implements Suggester<S> {

  static final char FUNC_PREFIX = '#';

  static final String REF_LABEL = FUNC_PREFIX + "container";
  static final String LIST_LABEL = FUNC_PREFIX + "item-list";
  static final String HELD_LABEL = FUNC_PREFIX + "held-item";
  static final String INV_LABEL = FUNC_PREFIX + "inventory";
  static final String HOTBAR_LABEL = FUNC_PREFIX + "hotbar";

  static final int HOTBAR_START = 0;
  static final int HOTBAR_END = 8;

  static final ArrayArgument<ItemStack> ITEMS_ARGUMENT
      = ArgumentTypes.array(Arguments.ITEMSTACK);

  static final ArgumentOption<Coordinate> X = Options.argument(CoordinateParser.X, "x");
  static final ArgumentOption<Coordinate> Y = Options.argument(CoordinateParser.Y, "y");
  static final ArgumentOption<Coordinate> Z = Options.argument(CoordinateParser.Z, "z");

  static final ArgumentOption<World> WORLD = Options.argument(ArgumentTypes.world(), "world");

  static final OptionsArgument OPTIONS = OptionsArgument.builder()
      .addRequired(X)
      .addRequired(Y)
      .addRequired(Z)
      .addOptional(WORLD)
      .build();

  final StringReader reader;

  @Nullable
  final CommandSource source;

  Suggester<S> suggestions;

  public ProviderParser(StringReader reader, @Nullable CommandSource source) {
    this.reader = reader;
    this.source = source;
  }

  ItemProvider parse() throws CommandSyntaxException {
    suggest(reader.getCursor(), (context, builder) -> {
      return Completions.suggest(builder, REF_LABEL, LIST_LABEL, HELD_LABEL, INV_LABEL);
    });

    if (!reader.canRead()) {
      separatorError();
    }

    if (reader.peek() != FUNC_PREFIX) {
      List<ItemStack> list = ITEMS_ARGUMENT.parse(reader);
      return new ItemListProvider(ItemLists.cloneAllItems(list));
    }

    int start = reader.getCursor();
    String label = Readers.readUntilWhitespace(reader);

    switch (label.toLowerCase()) {
      case REF_LABEL -> {
        ensureCanRead();
        suggest(reader.getCursor(), OPTIONS::listSuggestions);

        if (!reader.canRead()) {
          separatorError();
        }

        ParsedOptions options = OPTIONS.parse(reader);

        Coordinate x = options.getValue(X);
        Coordinate y = options.getValue(Y);
        Coordinate z = options.getValue(Z);
        World world = options.getValue(WORLD);

        return new PositionRefItemProvider(world == null ? null : world.getName(), x, y, z);
      }

      case LIST_LABEL -> {
        ensureCanRead();
        suggest(reader.getCursor(), ITEMS_ARGUMENT::listSuggestions);

        if (!reader.canRead()) {
          separatorError();
        }

        List<ItemStack> list = ITEMS_ARGUMENT.parse(reader);
        return new ItemListProvider(ItemLists.cloneAllItems(list));
      }

      case HELD_LABEL -> {
        ensureSourcePresent();

        Player player = source.asPlayer();
        ItemStack held = Commands.getHeldItem(player);
        ItemList list = ItemLists.newList(held.clone());

        return new ItemListProvider(list);
      }

      case HOTBAR_LABEL -> {
        ensureSourcePresent();

        Player player = source.asPlayer();
        Inventory inventory = player.getInventory();
        ItemList list = ItemLists.newList();

        for (int i = HOTBAR_START; i < HOTBAR_END; i++) {
          ItemStack item = inventory.getItem(i);

          if (ItemStacks.isEmpty(item)) {
            continue;
          }

          list.add(item.clone());
        }

        if (list.isEmpty()) {
          throw Exceptions.create("No items found in your hotbar");
        }

        return new ItemListProvider(list);
      }

      case INV_LABEL -> {
        ensureSourcePresent();

        Player player = source.asPlayer();
        Inventory inventory = player.getInventory();
        ItemList list = ItemLists.cloneAllItems(ItemLists.fromInventory(inventory));

        if (list.isEmpty()) {
          throw Exceptions.create("No items found in your inventory");
        }

        return new ItemListProvider(list);
      }

      default -> {
        reader.setCursor(start);
        throw Exceptions.formatWithContext("Unknown label '{0}'", reader, label);
      }
    }
  }

  void ensureSourcePresent() throws CommandSyntaxException {
    if (source == null) {
      throw Exceptions.create("Player not available in current context");
    }
  }

  void ensureCanRead() throws CommandSyntaxException {
    if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
      separatorError();
    }

    reader.skipWhitespace();
  }

  void separatorError() throws CommandSyntaxException {
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
        .dispatcherExpectedArgumentSeparator()
        .createWithContext(reader);
  }

  void suggest(int cursor, Suggester<S> suggester) {
    suggestions = (context, builder) -> {
      if (builder.getStart() != cursor) {
        builder = builder.createOffset(cursor);
      }

      return suggester.getSuggestions(context, builder);
    };
  }

  Suggester<S> defaultSuggestions() {
    return (context, builder) -> {
      return Completions.suggest(
          builder,
          REF_LABEL, LIST_LABEL, HELD_LABEL, INV_LABEL, HOTBAR_LABEL
      );
    };
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    if (suggestions == null) {
      suggestions = defaultSuggestions();
    }

    return suggestions.getSuggestions(context, builder);
  }
}
