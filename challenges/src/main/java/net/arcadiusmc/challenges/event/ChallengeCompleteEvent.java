package net.arcadiusmc.challenges.event;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.challenges.Challenge;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class ChallengeCompleteEvent extends UserEvent implements Cancellable {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Holder<Challenge> holder;

  private boolean cancelled;

  public ChallengeCompleteEvent(User user, Holder<Challenge> holder) {
    super(user);
    this.holder = holder;
  }

  public Challenge getChallenge() {
    return getHolder().getValue();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
