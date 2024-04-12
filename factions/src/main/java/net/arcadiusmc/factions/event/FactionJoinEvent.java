package net.arcadiusmc.factions.event;

import lombok.Getter;
import net.arcadiusmc.factions.Faction;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FactionJoinEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  @Getter
  private final Faction faction;

  public FactionJoinEvent(User user, Faction faction) {
    super(user);
    this.faction = faction;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
