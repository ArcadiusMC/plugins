package net.arcadiusmc.waypoints.event;

import lombok.Getter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.visit.WaypointVisit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class WaypointVisitEvent extends Event {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final User user;
  private final Location teleportDestination;

  private final EventType type;

  public WaypointVisitEvent(User user, Location teleportDestination, EventType type) {
    this.user = user;
    this.teleportDestination = teleportDestination;
    this.type = type;
  }

  public WaypointVisitEvent(WaypointVisit visit, EventType type) {
    this(visit.getUser(), visit.getTeleportLocation(), type);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }

  public enum EventType {
    ON_INSTANT_TELEPORT,
    ON_HULK_BEGIN,
    ON_TICK_UP,
    ON_TICK_DOWN,
    ON_LAND;
  }
}
