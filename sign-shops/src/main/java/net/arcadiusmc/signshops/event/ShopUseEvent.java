package net.arcadiusmc.signshops.event;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShopSession;
import net.arcadiusmc.user.User;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ShopUseEvent extends Event implements Cancellable {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final User user;
  private final SignShop shop;
  private final SignShopSession session;

  @Getter @Setter
  private boolean cancelled;

  public ShopUseEvent(User user, SignShopSession session) {
    this.user = user;
    this.shop = session.getShop();
    this.session = session;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
