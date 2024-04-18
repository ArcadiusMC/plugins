package net.arcadiusmc.markets.command;

import com.google.common.base.Strings;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.ValueModifierList;
import net.arcadiusmc.markets.ValueModifierList.Modifier;
import net.arcadiusmc.markets.ValueModifierList.ModifierOp;
import net.arcadiusmc.markets.command.MarketListArgument.Result;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;

public class ModifierListCommand extends BaseCommand {

  static final ArgumentOption<Float> AMOUNT
      = Options.argument(FloatArgumentType.floatArg(), "amount");

  static final ArgumentOption<ModifierOp> OP
      = Options.argument(ArgumentTypes.enumType(ModifierOp.class), "op");

  static final ArgumentOption<String> DISPLAY_NAME
      = Options.argument(StringArgumentType.string(), "display-name");

  static final ArgumentOption<Duration> LENGTH
      = Options.argument(ArgumentTypes.time(), "length");

  static final ArgumentOption<String> TAG
      = Options.argument(StringArgumentType.string(), "tag");

  static final OptionsArgument OPTIONS = OptionsArgument.builder()
      .addRequired(OP)
      .addRequired(AMOUNT)
      .addRequired(DISPLAY_NAME)
      .addOptional(LENGTH)
      .addOptional(TAG)
      .build();

  private final Function<Market, ValueModifierList> listGetter;
  private final String messagePrefix;

  private final MarketsPlugin plugin;

  public ModifierListCommand(
      MarketsPlugin plugin,
      String name,
      Function<Market, ValueModifierList> listGetter,
      String messagePrefix
  ) {
    super(name);

    this.plugin = plugin;
    this.listGetter = listGetter;
    this.messagePrefix = messagePrefix;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("list <market>");
    factory.usage("remove <market> <index>");
    factory.usage("remove-tagged <tag>");
    factory.usage("clear <markets>");

    factory.usage("add <markets> amount=<value> op=<operation> display-name=<name> [length=<duration>] [tag=<string>]");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("list")
            .then(argument("market", MarketCommands.argument)
                .executes(c -> {
                  Market market = c.getArgument("market", Market.class);
                  ValueModifierList list = getList(market);

                  if (list.isEmpty()) {
                    throw Exceptions.NOTHING_TO_LIST.exception(c.getSource());
                  }

                  TextJoiner joiner = TextJoiner.newJoiner();
                  ListIterator<Modifier> it = list.getModifiers().listIterator();

                  CommandSource source = c.getSource();

                  while (it.hasNext()) {
                    Modifier n = it.next();
                    int index = it.nextIndex();

                    joiner.add(
                        Messages.render(messagePrefix, "list.format")
                            .addValue("index", index)
                            .addValue("modifier", n.displayText(source))
                            .create(source)
                    );
                  }

                  source.sendMessage(
                      Messages.render(messagePrefix, "list.header")
                          .addValue("list", joiner.asComponent())
                          .addValue("label", market.displayName())
                          .create(source)
                  );
                  return 0;
                })
            )
        )

        .then(literal("remove")
            .then(argument("market", MarketCommands.argument)
                .then(argument("index", IntegerArgumentType.integer(1))
                    .suggests((context, builder) -> {
                      Market market = context.getArgument("market", Market.class);
                      ValueModifierList list = getList(market);
                      int size = list.getModifiers().size();

                      if (size < 1) {
                        return builder.buildFuture();
                      }

                      String input = builder.getRemainingLowerCase();
                      for (int i = 1; i <= size; i++) {
                        String suggestion = i + "";

                        if (!Completions.matches(input, suggestion)) {
                          continue;
                        }

                        Modifier modifier = list.getModifiers().get(i - 1);
                        Message hover = Grenadier.toMessage(
                            modifier.displayText(context.getSource())
                        );

                        builder.suggest(suggestion, hover);
                      }

                      return builder.buildFuture();
                    })

                    .executes(c -> {
                      Market market = c.getArgument("market", Market.class);
                      int index = c.getArgument("index", Integer.class);

                      ValueModifierList list = getList(market);
                      int size = list.getModifiers().size();

                      Commands.ensureIndexValid(index, size);

                      list.getModifiers().remove(index - 1);

                      c.getSource().sendSuccess(
                          Messages.render(messagePrefix, "removed.index")
                              .addValue("index", index)
                              .addValue("label", market.displayName())
                              .create(c.getSource())
                      );
                      return 0;
                    })
                )
            )
        )

        .then(literal("remove-tagged")
            .then(argument("tag", Arguments.RESOURCE_KEY)
                .suggests((context, builder) -> {
                  Collection<Market> markets = plugin.getManager().getMarkets();
                  Set<String> tags = new HashSet<>();

                  for (Market market : markets) {
                    ValueModifierList list = getList(market);

                    for (Modifier modifier : list.getModifiers()) {
                      if (Strings.isNullOrEmpty(modifier.tag())) {
                        continue;
                      }

                      tags.add(modifier.tag());
                    }
                  }

                  return Completions.suggest(builder, tags);
                })

                .executes(c -> {
                  String tag = c.getArgument("tag", String.class);
                  Collection<Market> markets = plugin.getManager().getMarkets();

                  for (Market market : markets) {
                    ValueModifierList list = getList(market);
                    list.removeTagged(tag);
                  }

                  c.getSource().sendSuccess(
                      Messages.render(messagePrefix, "removed.tagged")
                          .addValue("tag", tag)
                          .create(c.getSource())
                  );
                  return 0;
                })
            )
        )

        .then(literal("clear")
            .then(argument("markets", MarketCommands.listArgument)
                .executes(c -> {
                  Result result = c.getArgument("markets", Result.class);
                  List<Market> markets = result.markets();

                  for (Market market : markets) {
                    ValueModifierList list = getList(market);
                    list.clear();
                  }

                  c.getSource().sendSuccess(
                      Messages.render(messagePrefix, "cleared")
                          .addValue("label", result.label())
                          .addValue("count", markets.size())
                          .create(c.getSource())
                  );
                  return 0;
                })
            )
        )

        .then(literal("add")
            .then(argument("markets", MarketCommands.listArgument)
                .then(argument("options", OPTIONS)
                    .executes(c -> {
                      ParsedOptions options = ArgumentTypes.getOptions(c, "options");
                      Result result = c.getArgument("markets", Result.class);
                      List<Market> markets = result.markets();

                      Duration length = options.getValue(LENGTH);
                      Instant ends = length == null ? null : Instant.now().plus(length);

                      float amount = options.getValue(AMOUNT);
                      ModifierOp op = options.getValue(OP);

                      String tag = options.getValue(TAG);
                      String displayName = options.getValue(DISPLAY_NAME);

                      Modifier modifier = new Modifier(amount, op, ends, tag, displayName);

                      for (Market market : markets) {
                        ValueModifierList list = getList(market);
                        list.add(modifier.copy());
                      }

                      c.getSource().sendSuccess(
                          Messages.render(messagePrefix, "added")
                              .addValue("label", result.label())
                              .addValue("count", markets.size())
                              .addValue("modifier", modifier.displayText(c.getSource()))
                              .create(c.getSource())
                      );
                      return 0;
                    })
                )
            )
        );
  }

  private ValueModifierList getList(Market market) {
    return listGetter.apply(market);
  }
}
