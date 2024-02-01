package net.arcadiusmc.titles.events;

import lombok.Getter;
import net.arcadiusmc.titles.RankTier;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class TierPostChangeEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final RankTier from;
  private final RankTier to;

  public TierPostChangeEvent(User user, RankTier from, RankTier to) {
    super(user);
    this.from = from;
    this.to = to;
  }

  public boolean isDemotion() {
    return to.ordinal() < from.ordinal();
  }

  public boolean isPromotion() {
    return to.ordinal() > from.ordinal();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}