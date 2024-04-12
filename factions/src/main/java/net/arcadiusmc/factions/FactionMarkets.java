package net.arcadiusmc.factions;

import com.google.common.base.Strings;
import java.util.Objects;
import net.arcadiusmc.markets.Eviction;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PluginUtil;
import net.kyori.adventure.text.Component;

public class FactionMarkets {

  static final String MARKET_EVICT_SOURCE = "FactionsSystem";

  public static boolean isEnabled() {
    return PluginUtil.isEnabled("Player-Markets");
  }

  static void onLeave(User user, Faction faction, String marketGroup) {
    Market market = Markets.getOwned(user);

    if (market == null || Strings.isNullOrEmpty(market.getGroupName())) {
      return;
    }

    if (!Objects.equals(marketGroup, market.getGroupName())) {
      return;
    }

    MarketsManager manager = Markets.getManager();
    MarketsPlugin plugin = manager.getPlugin();

    if (manager.isMarkedForEviction(market)) {
      return;
    }

    Component reason = Messages.render("factions.markets.evictionReason")
        .addValue("faction", faction.displayName(null))
        .asComponent();

    manager.beginEviction(
        market,
        MARKET_EVICT_SOURCE + ";" + faction.getKey(),
        reason,
        plugin.getAutoEvictions().getConfig().evictionDelay()
    );
  }

  static void stopMarketEviction(User user, String marketGroup, Faction faction) {
    MarketsManager manager = Markets.getManager();
    Market owned = Markets.getOwned(user);

    if (owned == null || !manager.isMarkedForEviction(owned)) {
      return;
    }

    if (!Objects.equals(marketGroup, owned.getGroupName())) {
      return;
    }

    Eviction eviction = manager.getEviction(owned);
    if (!eviction.getSource().equals(MARKET_EVICT_SOURCE + ";" + faction.getKey())) {
      return;
    }

    manager.stopEviction(owned);
  }
}
