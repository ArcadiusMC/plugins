package net.arcadiusmc.waypoints.util;

import net.arcadiusmc.waypoints.Waypoint;

public interface WaypointAction {

  void accept(Waypoint waypoint);

  void onFinish();
}
