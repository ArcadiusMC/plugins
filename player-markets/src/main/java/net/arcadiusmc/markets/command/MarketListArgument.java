package net.arcadiusmc.markets.command;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.command.MarketListArgument.Result;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.Completions;

public class MarketListArgument implements ArgumentType<Result> {

  private final MarketsPlugin plugin;
  private final MarketArgument argument;

  public MarketListArgument(MarketsPlugin plugin, MarketArgument argument) {
    this.plugin = plugin;
    this.argument = argument;
  }

  @Override
  public Result parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();
    Collection<Market> markets = plugin.getManager().getMarkets();

    if (reader.peek() == '*') {
      reader.skip();
      return new Result("*", new ArrayList<>(markets));
    }

    if (reader.peek() == '#') {
      reader.skip();;
      String label = Arguments.RESOURCE_KEY.parse(reader);
      List<Market> result = new ArrayList<>();

      for (Market market : markets) {
        if (Strings.isNullOrEmpty(market.getGroupName())) {
          continue;
        }
        if (!Objects.equals(label, market.getGroupName())) {
          continue;
        }

        result.add(market);
      }

      if (result.isEmpty()) {
        reader.setCursor(start);
        throw Messages.render("markets.errors.noneInGroup")
            .addValue("group", label)
            .exceptionWithContext(reader);
      }

      return new Result("#" + label, result);
    }

    Market market = argument.parse(reader);
    List<Market> resultList = new ArrayList<>();
    resultList.add(market);

    return new Result(market.getRegionName(), resultList);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    String input = builder.getRemainingLowerCase();
    Collection<Market> markets = plugin.getManager().getMarkets();

    Completions.suggest(builder, "*");

    if (input.isBlank() || input.startsWith("#")) {
      Set<String> groups = new HashSet<>();
      for (Market market : markets) {
        if (Strings.isNullOrEmpty(market.getGroupName())) {
          continue;
        }

        groups.add("#" + market.getGroupName());
      }

      Completions.suggest(builder, groups);
    }

    return argument.listSuggestions(context, builder);
  }

  public record Result(String label, List<Market> markets) {

  }
}
