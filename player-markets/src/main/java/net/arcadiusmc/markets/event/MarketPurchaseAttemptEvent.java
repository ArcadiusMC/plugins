package net.arcadiusmc.markets.event;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MarketPurchaseAttemptEvent extends UserEvent implements Cancellable {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  @Getter
  private final Market market;

  @Getter @Setter
  private Component denyReason;

  @Getter @Setter
  private boolean cancelled;

  public MarketPurchaseAttemptEvent(User user, Market market) {
    super(user);
    this.market = market;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
