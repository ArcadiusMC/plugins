package net.arcadiusmc.waypoints;

import com.google.common.base.Strings;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.webmap.MapIcon;
import net.arcadiusmc.webmap.MapLayer;
import net.arcadiusmc.webmap.MapMarker;
import net.arcadiusmc.webmap.MapPointMarker;
import net.arcadiusmc.webmap.WebMap;
import net.arcadiusmc.webmap.WebMaps;
import org.bukkit.World;
import org.slf4j.Logger;

/**
 * Methods relating to Waypoints and their Dynmap markers.
 */
public final class WaypointWebmaps {
  private WaypointWebmaps() {}

  private static final Logger LOGGER = Loggers.getLogger();

  /**
   * Waypoint Marker set ID
   */
  public static final String SET_ID = "waypoint_marker_set";

  /**
   * Waypoint marker set display name
   */
  public static final String SET_NAME = "Waypoints";

  /**
   * Updates the marker of the given waypoint.
   * <p>
   * If the waypoint doesn't have a name, the marker is deleted, if it exists.
   * <p>
   * If the marker should exist, but doesn't, it's created, if it does exist, it's data is updated
   * to be in sync with the actual waypoint
   */
  public static void updateMarker(Waypoint waypoint) {
    if (!WebMaps.isEnabled()) {
      return;
    }

    Optional<MapLayer> layerOpt = getSet(waypoint.getWorld());
    if (layerOpt.isEmpty()) {
      return;
    }

    MapLayer set = layerOpt.get();
    String name = waypoint.getEffectiveName();
    Optional<MapPointMarker> markerOpt = set.findPointMarker(waypoint.getMarkerId());
    MapPointMarker marker = markerOpt.orElse(null);

    if (Strings.isNullOrEmpty(name) || !waypoint.get(WaypointProperties.ALLOWS_MARKER)) {
      if (marker != null) {
        marker.delete();
      }

      return;
    }

    String iconImage = waypoint.get(WaypointProperties.MARKER_ICON);

    if (Strings.isNullOrEmpty(iconImage)) {
      LOGGER.warn("Waypoint {} has no icon image set, cannot create/update marker", waypoint);
      return;
    }

    Optional<MapIcon> iconOpt = getIcon(waypoint.getWorld(), iconImage);

    if (iconOpt.isEmpty()) {
      LOGGER.error("Couldn't find icon for waypoint marker! '{}'", iconImage);
      return;
    }

    MapIcon icon = iconOpt.get();

    int x = waypoint.getPosition().x();
    int y = waypoint.getPosition().y();
    int z = waypoint.getPosition().z();

    if (marker == null) {
      Result<MapPointMarker> markerResult = set.createPointMarker(
          waypoint.getMarkerId(),
          name,

          // Location
          x, y, z,

          // Icon
          icon
      );

      if (markerResult.isError()) {
        markerResult
            .mapError(string -> "Failed to create marker for waypoint " + waypoint + ": " + string)
            .applyError(LOGGER::error);

        return;
      }

      marker = markerResult.getValue();
    } else {
      marker.setTitle(name);
      marker.setIcon(icon);
      marker.setLocation(x, y, z);
    }

    marker.setDescription(name);
  }

  static void removeMarker(Waypoint waypoint) {
    getMarker(waypoint).ifPresent(MapMarker::delete);
  }

  static Optional<MapPointMarker> getMarker(Waypoint waypoint) {
    return getSet(waypoint.getWorld())
        .flatMap(mapLayer -> mapLayer.findPointMarker(waypoint.getMarkerId()));
  }

  static Optional<MapLayer> getSet(World world) {
    return WebMaps.findOrDefineLayer(world, SET_ID, SET_NAME);
  }

  static Optional<MapIcon> getIcon(World world, String id) {
    return WebMap.map().getIcon(world, id);
  }
}