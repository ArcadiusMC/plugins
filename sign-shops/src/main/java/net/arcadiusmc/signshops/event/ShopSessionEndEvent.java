package net.arcadiusmc.signshops.event;

import lombok.Getter;
import net.arcadiusmc.signshops.SignShopSession;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ShopSessionEndEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  @Getter
  private final SignShopSession session;

  public ShopSessionEndEvent(User user, SignShopSession session) {
    super(user);
    this.session = session;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
