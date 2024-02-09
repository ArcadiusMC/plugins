package net.arcadiusmc.economy.signshops.event;

import lombok.Getter;
import net.arcadiusmc.economy.ShopsPlugin;
import net.arcadiusmc.economy.market.MarketManager;
import net.arcadiusmc.economy.market.MarketShop;
import net.arcadiusmc.economy.signshops.SignShop;
import net.arcadiusmc.user.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ShopPostUseEvent extends Event {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final User user;
  private final SignShop shop;
  private final MarketShop marketShop;

  public ShopPostUseEvent(User user, SignShop shop) {
    this.user = user;
    this.shop = shop;

    MarketManager manager = ShopsPlugin.getPlugin().getMarkets();
    this.marketShop = manager.get(shop.getPosition());
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
