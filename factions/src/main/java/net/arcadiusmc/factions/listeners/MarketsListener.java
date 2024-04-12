package net.arcadiusmc.factions.listeners;

import com.google.common.base.Strings;
import java.util.Objects;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionManager;
import net.arcadiusmc.factions.FactionMember;
import net.arcadiusmc.factions.Properties;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.event.MarketPurchaseAttemptEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MarketsListener implements Listener {

  private final FactionManager manager;

  public MarketsListener(FactionManager manager) {
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true)
  public void onMarketPurchaseAttempt(MarketPurchaseAttemptEvent event) {
    User user = event.getUser();
    Market market = event.getMarket();
    Faction faction = findFromGroup(market.getGroupName());

    if (faction == null) {
      return;
    }

    FactionMember member = faction.getActiveMember(user.getUniqueId());
    if (member != null) {
      return;
    }

    event.setCancelled(true);
    event.setDenyReason(
        Messages.render("factions.markets.purchaseDeny")
            .addValue("faction", faction.displayName(user))
            .create(user)
    );
  }

  private Faction findFromGroup(String groupName) {
    if (Strings.isNullOrEmpty(groupName)) {
      return null;
    }

    for (Faction faction : manager.getFactions()) {
      String factionGroup = faction.get(Properties.MARKET_GROUP);

      if (Strings.isNullOrEmpty(factionGroup)) {
        continue;
      }

      if (!Objects.equals(factionGroup, groupName)) {
        continue;
      }

      return faction;
    }

    return null;
  }
}
