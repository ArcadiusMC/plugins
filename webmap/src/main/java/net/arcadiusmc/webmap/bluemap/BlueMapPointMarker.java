package net.arcadiusmc.webmap.bluemap;

import com.mojang.datafixers.util.Unit;
import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.markers.POIMarker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.webmap.MapIcon;
import net.arcadiusmc.webmap.MapPointMarker;
import org.slf4j.Logger;

public class BlueMapPointMarker extends BlueMapMarker implements MapPointMarker {

  private static final Logger LOGGER = Loggers.getLogger();

  private final POIMarker marker;

  public BlueMapPointMarker(BlueMapLayer layer, String id, POIMarker marker) {
    super(layer, id, marker);
    this.marker = marker;
  }

  @Override
  public double x() {
    return marker.getPosition().getX();
  }

  @Override
  public double y() {
    return marker.getPosition().getY();
  }

  @Override
  public double z() {
    return marker.getPosition().getZ();
  }

  @Override
  public void setLocation(double x, double y, double z) {
    marker.setPosition(x, y, z);
  }

  @Override
  public MapIcon getIcon() {
    String address = marker.getIconAddress();
    return new BlueMapIcon(address, address, layer.map);
  }

  static void copyAsset(String path, AssetStorage from, AssetStorage to) throws IOException {
    Optional<InputStream> opt = from.readAsset(path);

    if (opt.isEmpty()) {
      return;
    }

    try (InputStream input = opt.get()) {
      try (OutputStream out = to.writeAsset(path)) {
        input.transferTo(out);
      }
    }
  }

  @Override
  public Result<Unit> setIcon(MapIcon icon) {
    if (icon == null) {
      return Result.error("Null icon");
    }
    if (!(icon instanceof BlueMapIcon blu)) {
      return Result.error("Icon from a different implementation (How???)");
    }

    if (!blu.map.getId().equals(layer.map.getId())) {
      try {
        copyAsset(blu.path, blu.storage, layer.map.getAssetStorage());
      } catch (IOException exc) {
        LOGGER.error("Error copying bluemap icon {} from world {} to {}",
            blu.path, blu.map.getId(), layer.map.getId(),
            exc
        );
      }
    }

    marker.setIcon(blu.path, marker.getAnchor());
    return Result.unit();
  }
}
