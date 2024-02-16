package net.arcadiusmc.punish.event;

import lombok.Getter;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class UserPunishedEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Punishment punishment;

  public UserPunishedEvent(User user, Punishment punishment) {
    super(user);
    this.punishment = punishment;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
