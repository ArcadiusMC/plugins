package net.arcadiusmc.core.commands.item;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.loader.MessageRender;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public class ItemLoreNode extends ItemModifierNode {

  public ItemLoreNode() {
    super(
        "lore",
        "itemlore", "lores", "itemlores"
    );
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("clear")
        .addInfo("Clears your held item's lore");

    ItemNameNode.namingNote(
        factory.usage("add <text>")
            .addInfo("Adds <text> to your held item's lore")
    );

    factory.usage("display")
        .addInfo("Displays the lore of the item you're holding with index numbers");

    factory.usage("set <index> <text>")
        .addInfo("Sets the lore on the specified line of the item you're holding");

    factory.usage("remove <indices>")
        .addInfo("Removes the lore on the given line");
  }

  @Override
  public void create(LiteralArgumentBuilder<CommandSource> command) {
    command
        .then(literal("display")
            .executes(this::displayLore)
        )

        .then(literal("clear")
            .executes(c -> {
              CommandSource source = c.getSource();

              ItemStack held = getHeld(source);
              held.lore(null);

              source.sendSuccess(ItemMessages.LORE_CLEARED.renderText(source));
              return 0;
            })
        )

        .then(literal("add")
            .then(argument("text", Arguments.CHAT)
                .executes(this::addLore)
            )
        )

        .then(literal("set")
            .then(argument("index", IntegerArgumentType.integer(1))
                .suggests(suggestLoreIndexes())

                .then(argument("text", Arguments.CHAT)
                    .executes(this::setLore)
                )
            )
        )

        .then(literal("remove")
            .then(argument("indices", ArgumentTypes.intRange())
                .suggests(suggestLoreIndexes())

                .executes(this::removeLore)
            )
        );
  }

  private SuggestionProvider<CommandSource> suggestLoreIndexes() {
    return (context, builder) -> {
      ItemStack held = getHeld(context.getSource());
      List<Component> lore = held.lore();

      if (lore == null || lore.isEmpty()) {
        return Suggestions.empty();
      }

      String token = builder.getRemainingLowerCase();

      for (int i = 1; i <= lore.size(); i++) {
        String suggestion = i + "";

        if (Completions.matches(token, suggestion)) {
          builder.suggest(suggestion);
        }
      }

      return builder.buildFuture();
    };
  }

  private int removeLore(CommandContext<CommandSource> c)
      throws CommandSyntaxException
  {
    CommandSource source = c.getSource();
    IntRange removeRange = c.getArgument("indices", IntRange.class);

    ItemStack held = getHeld(source);
    List<Component> lore = held.lore();

    if (lore == null || lore.isEmpty()) {
      throw ItemMessages.NO_LORE.exception(source);
    }

    int size = lore.size();

    int minIndex = removeRange.min().orElse(0);
    int maxIndex = removeRange.max().orElse(size);

    Commands.ensureIndexValid(minIndex, size);
    Commands.ensureIndexValid(maxIndex, size);

    lore.subList(minIndex - 1, maxIndex).clear();
    held.lore(lore);

    boolean singleIndex = removeRange.isExact();

    MessageRender render = singleIndex
        ? ItemMessages.REMOVED_LORE_INDEX.get()
        : ItemMessages.REMOVED_LORE_RANGE.get();

    source.sendSuccess(render.addValue("index", removeRange).create(source));
    return 0;
  }

  private int addLore(CommandContext<CommandSource> c) throws CommandSyntaxException {
    CommandSource source = c.getSource();

    ItemStack held = getHeld(source);
    List<Component> lore = held.lore();

    if (lore == null) {
      lore = new ArrayList<>();
    }

    Component message = Arguments.getMessage(c, "text").asComponent();

    lore.add(optionallyWrap(message, c, "text"));
    held.lore(lore);

    source.sendSuccess(
        ItemMessages.ADDED_LORE.get()
            .addValue("lore", message)
            .create(source)
    );
    return 0;
  }

  private int setLore(CommandContext<CommandSource> c) throws CommandSyntaxException {
    CommandSource source = c.getSource();

    ItemStack held = getHeld(source);
    List<Component> lore = held.lore();

    if (lore == null || lore.isEmpty()) {
      throw ItemMessages.NO_LORE.exception(source);
    }

    int index = c.getArgument("index", Integer.class);
    Component text = Arguments.getMessage(c, "text").asComponent();

    Commands.ensureIndexValid(index, lore.size());

    lore.set(index - 1, optionallyWrap(text, c, "text"));
    held.lore(lore);

    source.sendSuccess(
        ItemMessages.SET_LORE.get()
            .addValue("index", index)
            .addValue("lore", text)
            .create(source)
    );
    return 0;
  }

  private int displayLore(CommandContext<CommandSource> c) throws CommandSyntaxException {
    CommandSource source = c.getSource();

    ItemStack held = getHeld(source);
    List<Component> lore = held.lore();

    if (lore == null || lore.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    TextJoiner joiner = TextJoiner.onNewLine();

    for (int i = 0; i < lore.size(); i++) {
      Component line = lore.get(i);
      int viewerIndex = i + 1;

      joiner.add(
          ItemMessages.LORE_DISPLAY_LINE.get()
              .addValue("rawLine", i)
              .addValue("line", viewerIndex)
              .addValue("line.content", line)
              .create(source)
      );
    }

    source.sendMessage(
        ItemMessages.LORE_DISPLAY.get()
            .addValue("lines", joiner.asComponent())
            .create(source)
    );
    return 0;
  }
}