package net.arcadiusmc.markets;

import java.util.UUID;
import net.arcadiusmc.holograms.Holograms;
import net.arcadiusmc.holograms.ScoreMapSource;
import net.arcadiusmc.utils.ScoreIntMap;

class MarketLeaderboards {

  static void registerAll(MarketsPlugin plugin) {
    ScoreIntMap<UUID> debts = plugin.getDebts().getDebts();
    ScoreMapSource source = new ScoreMapSource(debts);

    Holograms.ifLoaded(service -> {
      service.getSources().register("market_debts", source);
    });
  }
}
