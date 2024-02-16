package net.arcadiusmc.punish.event;

import lombok.Getter;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import net.forthecrown.grenadier.CommandSource;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class UserPardonedEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Punishment punishment;
  private final CommandSource pardoner;

  public UserPardonedEvent(User user, Punishment punishment, CommandSource pardoner) {
    super(user);

    this.punishment = punishment;
    this.pardoner = pardoner;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
