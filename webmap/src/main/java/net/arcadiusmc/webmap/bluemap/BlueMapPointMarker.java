package net.arcadiusmc.webmap.bluemap;

import com.mojang.datafixers.util.Unit;
import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.markers.POIMarker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.webmap.MapIcon;
import net.arcadiusmc.webmap.MapPointMarker;

public class BlueMapPointMarker extends BlueMapMarker implements MapPointMarker {

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
    AssetStorage storage = layer.map.getAssetStorage();

    return new BlueMapIcon(address, storage);
  }

  @Override
  public Result<Unit> setIcon(MapIcon icon) {
    if (icon == null) {
      return Result.error("Null icon");
    }
    if (!(icon instanceof BlueMapIcon blu)) {
      return Result.error("Icon from a different implementation (How???)");
    }

    if (blu.storage != null) {
      marker.setIcon(blu.path, marker.getAnchor());
      return Result.unit();
    }

    AssetStorage assets = layer.map.getAssetStorage();
    String path = blu.id + ".png";

    try {
      if (!assets.assetExists(path)) {
        try (
            OutputStream out = assets.writeAsset(path);
            InputStream in = Files.newInputStream(blu.index.getIconPath(blu.id))
        ) {
          in.transferTo(out);
        }
      }
    } catch (IOException exc) {
      return Result.error("IO error trying to set icon: %s", exc.getMessage());
    }

    marker.setIcon(path, marker.getAnchor());
    return Result.unit();
  }
}
