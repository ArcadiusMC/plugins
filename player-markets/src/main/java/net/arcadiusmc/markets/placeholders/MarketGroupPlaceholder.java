package net.arcadiusmc.markets.placeholders;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

interface MarketGroupPlaceholder extends TextPlaceholder {

  @Override
  default @Nullable Component render(String match, PlaceholderContext render) {
    MarketsManager manager = Markets.getManager();
    Collection<Market> markets = new ArrayList<>(manager.getMarkets());

    if (!Strings.isNullOrEmpty(match)) {
      markets.removeIf(market -> {
        if (Strings.isNullOrEmpty(market.getGroupName())) {
          return true;
        }

        String group = market.getGroupName();
        return !Objects.equals(match, group);
      });
    }

    return render(markets, render);
  }

  Component render(Collection<Market> markets, PlaceholderContext render);
}
