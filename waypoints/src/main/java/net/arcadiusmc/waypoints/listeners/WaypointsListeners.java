package net.arcadiusmc.waypoints.listeners;

import static net.arcadiusmc.events.Events.register;

public final class WaypointsListeners {
  private WaypointsListeners() {}

  public static void registerAll() {
    register(new AutoGenListener());
    register(new DayChangeListener());
    register(new HomeListener());
    register(new PlayerJoinListener());
    register(new PlayerListener());
    register(new ServerListener());
    register(new WaypointDestroyListener());
    register(new WaypointListener());
  }
}