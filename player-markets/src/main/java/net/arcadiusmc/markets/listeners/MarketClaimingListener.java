package net.arcadiusmc.markets.listeners;

import com.google.common.base.Strings;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.markets.Entrance;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.markets.gui.ClaimingBook;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.inventory.Book;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

public class MarketClaimingListener implements Listener {

  private static final Logger LOGGER = Loggers.getLogger();

  private final MarketsPlugin plugin;

  public MarketClaimingListener(MarketsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    PersistentDataContainer pdc = entity.getPersistentDataContainer();

    String marketName = pdc.get(Entrance.KEY, PersistentDataType.STRING);

    if (Strings.isNullOrEmpty(marketName)) {
      return;
    }

    Market market = plugin.getManager().getMarket(marketName);

    if (market == null) {
      LOGGER.warn("Cannot find market named '{}' for shop entrance! at={}",
          marketName, entity.getLocation()
      );

      return;
    }

    User user = Users.get(event.getPlayer());
    Book book = ClaimingBook.create(market, user);

    user.openBook(book);
  }
}
