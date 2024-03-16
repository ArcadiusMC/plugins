package net.arcadiusmc.waypoints;

import com.google.common.base.Strings;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.webmap.MapIcon;
import net.arcadiusmc.webmap.MapLayer;
import net.arcadiusmc.webmap.MapMarker;
import net.arcadiusmc.webmap.MapPointMarker;
import net.arcadiusmc.webmap.WebMaps;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * Methods relating to Waypoints and their Dynmap markers.
 */
@UtilityClass
class WaypointWebmaps {

  private static final Logger LOGGER = Loggers.getLogger();

  /**
   * Waypoint Marker set ID
   */
  public static final String SET_ID = "waypoint_marker_set";

  /**
   * Waypoint marker set display name
   */
  public static final String SET_NAME = "Waypoints";

  public static final String NORMAL_LABEL = "region_pole_normal";

  public static final String SPECIAL_LABEL = "region_pole_special";

  /**
   * Updates the marker of the given waypoint.
   * <p>
   * If the waypoint doesn't have a name, the marker is deleted, if it exists.
   * <p>
   * If the marker should exist, but doesn't, it's created, if it does exist, it's data is updated
   * to be in sync with the actual waypoint
   */
  static void updateMarker(Waypoint waypoint) {
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

    Optional<MapIcon> iconOpt = getIcon(iconImage);

    if (iconOpt.isEmpty()) {
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

  static Optional<MapIcon> getIcon(String id) {
    return WebMaps.findOrDefineIcon(id, id, () -> {
      var plugin = JavaPlugin.getPlugin(WaypointsPlugin.class);
      return plugin.getResource(id + ".png");
    });
  }
}