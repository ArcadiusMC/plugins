package net.arcadiusmc.webmap.bluemap;

import com.mojang.datafixers.util.Unit;
import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.BlueMapMap;
import java.io.IOException;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.webmap.MapIcon;
import org.slf4j.Logger;

public class BlueMapIcon implements MapIcon {

  private static final Logger LOGGER = Loggers.getLogger();

  final String id;
  final String path;
  final AssetStorage storage;
  final BlueMapMap map;

  public BlueMapIcon(String id, String path, BlueMapMap map) {
    this.id = id;
    this.path = path;
    this.map = map;
    this.storage = map.getAssetStorage();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public Result<Unit> setName(String name) {
    return Result.error("Not-implemented");
  }

  @Override
  public void delete() {
    try {
      storage.deleteAsset(path);
    } catch (IOException exc) {
      LOGGER.error("Error deleting icon asset", exc);
    }
  }
}
