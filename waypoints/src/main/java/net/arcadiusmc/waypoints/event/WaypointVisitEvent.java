package net.arcadiusmc.waypoints.event;

import lombok.Getter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import net.arcadiusmc.waypoints.Waypoint;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class WaypointVisitEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Location destination;
  private final Location currentLocation;

  private final EventType type;
  private final Waypoint waypoint;

  public WaypointVisitEvent(
      User user,
      Location destination,
      Location currentLocation,
      EventType type,
      Waypoint waypoint
  ) {
    super(user);
    this.destination = destination;
    this.currentLocation = currentLocation;
    this.type = type;
    this.waypoint = waypoint;
  }

  public Location getDestination() {
    return destination.clone();
  }

  public Location getCurrentLocation() {
    return currentLocation.clone();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }

  public enum EventType {
    INSTANT_TELEPORT,
    HULK_BEGIN,
    TICK_UP,
    TICK_DOWN,
    LAND;
  }
}
