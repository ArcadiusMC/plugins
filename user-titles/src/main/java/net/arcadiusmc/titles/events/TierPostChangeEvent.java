package net.arcadiusmc.titles.events;

import lombok.Getter;
import net.arcadiusmc.titles.Tier;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class TierPostChangeEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Tier from;
  private final Tier to;

  public TierPostChangeEvent(User user, Tier from, Tier to) {
    super(user);
    this.from = from;
    this.to = to;
  }

  public boolean isDemotion() {
    return to.isLesserThan(from);
  }

  public boolean isPromotion() {
    return to.isGreaterThan(from);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}