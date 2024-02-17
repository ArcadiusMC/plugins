package net.arcadiusmc.signshops.event;

import lombok.Getter;
import net.arcadiusmc.signshops.SignShop;
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

  public ShopPostUseEvent(User user, SignShop shop) {
    this.user = user;
    this.shop = shop;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
