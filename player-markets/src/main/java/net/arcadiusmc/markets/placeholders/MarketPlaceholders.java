package net.arcadiusmc.markets.placeholders;

import java.util.function.ToIntFunction;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.PlaceholderList;
import net.arcadiusmc.text.placeholder.PlaceholderService;
import net.arcadiusmc.text.placeholder.Placeholders;

public final class MarketPlaceholders {
  private MarketPlaceholders() {}

  static final String EARNINGS_MONTHLY = "markets.monthlyEarnings";
  static final String OWNED_SHOP_COUNT = "markets.ownedShops";
  static final String TOTAL_SHOP_COUNT = "markets.totalShops";

  static final MarketGroupPlaceholder EARNINGS
      = summing(Market::getEarnings);

  static final MarketGroupPlaceholder OWNED_SHOPS
      = summing(value -> value.getOwnerId() == null ? 0 : 1);

  static final MarketGroupPlaceholder TOTAL_SHOPS = (markets, render) -> {
    return Text.formatNumber(markets.size());
  };

  public static void registerAll() {
    PlaceholderService service = Placeholders.getService();
    PlaceholderList list = service.getDefaults();

    list.add(EARNINGS_MONTHLY, EARNINGS);
    list.add(OWNED_SHOP_COUNT, OWNED_SHOPS);
    list.add(TOTAL_SHOP_COUNT, TOTAL_SHOPS);
  }

  public static void unregisterAll() {
    PlaceholderService service = Placeholders.getService();
    PlaceholderList list = service.getDefaults();

    list.remove(EARNINGS_MONTHLY);
    list.remove(OWNED_SHOP_COUNT);
    list.remove(TOTAL_SHOP_COUNT);
  }

  static MarketGroupPlaceholder summing(ToIntFunction<Market> getter) {
    return (markets, render) -> {
      int sum = 0;

      for (Market market : markets) {
        sum += getter.applyAsInt(market);
      }

      return Text.formatNumber(sum);
    };
  }
}
