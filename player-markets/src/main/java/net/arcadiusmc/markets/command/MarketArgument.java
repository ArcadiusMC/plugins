package net.arcadiusmc.markets.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.markets.MExceptions;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.internal.SimpleVanillaMapped;

public class MarketArgument implements ArgumentType<Market>, SimpleVanillaMapped {

  private final MarketsPlugin plugin;

  public MarketArgument(MarketsPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public Market parse(StringReader reader) throws CommandSyntaxException {
    MarketsManager manager = plugin.getManager();

    int start = reader.getCursor();

    String word = reader.readUnquotedString();
    Market market = manager.getMarket(word);

    if (market != null) {
      return market;
    }

    reader.setCursor(start);
    throw MExceptions.unknownMarket(word, reader);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    Collection<Market> markets = plugin.getManager().getMarkets();
    return Completions.suggest(builder, markets.stream().map(Market::getRegionName));
  }

  @Override
  public ArgumentType<?> getVanillaType() {
    return Arguments.USER.getVanillaType();
  }
}
