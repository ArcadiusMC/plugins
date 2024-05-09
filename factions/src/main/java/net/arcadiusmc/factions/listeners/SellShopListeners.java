package net.arcadiusmc.factions.listeners;

import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.factions.FactionManager;
import net.arcadiusmc.factions.FactionMember;
import net.arcadiusmc.factions.Factions;
import net.arcadiusmc.factions.FactionsConfig;
import net.arcadiusmc.sellshop.event.ItemPriceCalculateEvent;
import net.arcadiusmc.user.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SellShopListeners implements Listener {
  static final String PREFIX = "faction:";

  @EventHandler(ignoreCancelled = true)
  public void onItemPriceCalculate(ItemPriceCalculateEvent event) {
    FactionManager manager = Factions.getManager();
    Faction faction = null;

    for (String tag : event.getTags()) {
      if (!tag.startsWith(PREFIX)) {
        continue;
      }

      String factionName = tag.substring(PREFIX.length());
      faction = manager.getFaction(factionName);

      if (faction != null) {
        break;
      }
    }

    if (faction == null) {
      return;
    }

    User user = event.getUser();
    FactionsConfig config = Factions.getConfig();
    FactionMember member = faction.getActiveMember(user.getUniqueId());
    float rep;

    if (member == null) {
      rep = config.getStartingReputation();
    } else {
      rep = member.getReputation();
    }

    float minMultiplier = config.getMinSellShopMultiplier();
    float maxMultiplier = config.getMaxSellShopMultiplier();

    float minRep = config.getMinReputation();
    float maxRep = config.getMaxReputation();
    float dif = maxRep - minRep;

    float prog = ((rep + Math.abs(minRep)) / dif);
    float multiplier = minMultiplier + ((maxMultiplier - minMultiplier) * prog);

    event.setEarned((int) (event.getEarned() * multiplier));
  }
}
