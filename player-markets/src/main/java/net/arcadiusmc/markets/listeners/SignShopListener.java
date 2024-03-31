package net.arcadiusmc.markets.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsManager;
import net.arcadiusmc.markets.MarketsPlugin;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShopSession;
import net.arcadiusmc.signshops.event.ShopSessionEndEvent;
import net.arcadiusmc.signshops.event.ShopUseEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.math.WorldVec3i;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SignShopListener implements Listener {

  private final MarketsPlugin plugin;

  private final Map<SignShopSession, SessionTax> taxMap = new HashMap<>();

  public SignShopListener(MarketsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onShopUse(ShopUseEvent event) {
    MarketsManager manager = plugin.getManager();
    SignShop shop = event.getShop();
    WorldVec3i pos = shop.getPosition();

    // Only [Buy] shop interactions are taxed (Because they're the most common)
    if (!shop.getType().isBuyType()) {
      return;
    }

    // Admin shops don't count for taxes, and neither do
    // self-purchases
    if (shop.getOwner() == null || event.getUser().getUniqueId().equals(shop.getOwner())) {
      return;
    }

    Collection<Market> overlapping = manager.getOverlapping(pos.getWorld(), pos.getPos());
    int price = event.getSession().getPrice();

    Market market = findTaxedMarket(overlapping, shop);

    if (market == null) {
      return;
    }

    User customer = event.getSession().getCustomer();
    if (market.getBannedCustomers().contains(customer.getUniqueId())) {
      event.setCancelled(true);
      return;
    }

    User owner = Users.get(market.getOwnerId());

    float taxRate = market.getTaxRate();
    int taxed = (int) (price * taxRate);

    if (taxed > 0) {
      owner.removeBalance(taxed);

      SessionTax tax = taxMap.computeIfAbsent(event.getSession(), s -> new SessionTax());
      tax.taxed += taxed;
      tax.rate = taxRate;
    }

    Market merged = market.getMerged();

    market.addEarnings(price);

    if (merged != null) {
      merged.addEarnings(price);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onShopSessionEnd(ShopSessionEndEvent event) {
    SessionTax tax = taxMap.remove(event.getSession());

    if (tax == null || tax.taxed < 1) {
      return;
    }

    UUID ownerId = event.getSession().getShop().getOwner();
    if (ownerId == null) {
      return;
    }

    User owner = Users.get(ownerId);

    float rate = tax.rate;

    Component message = Messages.render("markets.taxed")
        .addValue("rate", rate)
        .addValue("taxed", tax.taxed)
        .create(owner);

    Mail.sendOrMail(owner, message);
  }

  Market findTaxedMarket(Collection<Market> markets, SignShop shop) {
    List<Market> list = new ArrayList<>(markets);
    list.removeIf(market -> {
      if (shop.getOwner().equals(market.getOwnerId())) {
        return false;
      }
      return !market.getMembers().contains(shop.getOwner());
    });

    if (list.isEmpty()) {
      return null;
    }

    if (list.size() == 1) {
      return markets.iterator().next();
    }

    Comparator<Market> comparator = Comparator.comparingInt(market -> {
      return computeScore(market, shop.getOwner());
    });

    list.sort(comparator);
    return list.get(0);
  }

  int computeScore(Market market, UUID signShopOwner) {
    // Lower score means market comes first
    int score = 0;

    if (Objects.equals(signShopOwner, market.getOwnerId())) {
      score -= 2;
    }

    if (market.getMembers().contains(signShopOwner)) {
      score -= 1;
    }

    Market merged=  market.getMerged();
    if (merged != null) {
      score += computeScore(market, signShopOwner);
    }

    return score;
  }

  private static class SessionTax {
    int taxed;
    float rate;
  }
}
