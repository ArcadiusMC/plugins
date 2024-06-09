package net.arcadiusmc.webmap.bluemap;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.webmap.MapIcon;
import net.arcadiusmc.webmap.MapLayer;
import net.arcadiusmc.webmap.WebMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class BlueWebmap implements WebMap {

  private static final Logger LOGGER = Loggers.getLogger();

  private final IconIndex iconIndex;

  private final Map<String, MarkerSetInfo> pluginSets = new Object2ObjectOpenHashMap<>();
  private final Path markerJson;

  public BlueWebmap(Path pluginDir) {
    this.iconIndex = new IconIndex(pluginDir.resolve("bluemap-icons"));
    this.markerJson = pluginDir.resolve("markers.json");

    BlueMapAPI.onEnable(blueMapAPI -> load());
  }

  private Optional<BlueMapAPI> api() {
    return BlueMapAPI.getInstance();
  }

  private Optional<BlueMapMap> getMap(Object world) {
    if (world == null) {
      return Optional.empty();
    }

    return api()
        .flatMap(api -> api.getWorld(world))
        .map(w -> {
          var maps = w.getMaps();

          if (maps.isEmpty()) {
            return (BlueMapMap) null; // Don't let IntelliJ fool you, this cast is required
          }

          return maps.iterator().next();
        });
  }

  @Override
  public Optional<MapLayer> getLayer(@NotNull World world, String id) {
    if (world == null || Strings.isNullOrEmpty(id)) {
      return Optional.empty();
    }

    return getMap(world)
        .map(blueMapMap -> {
          MarkerSet set = blueMapMap.getMarkerSets().get(id);

          if (set == null) {
            return null;
          }

          return new BlueMapLayer(id, set, world, blueMapMap, this);
        });
  }

  @Override
  public Result<MapLayer> createLayer(@NotNull World world, String id, String name) {
    if (Strings.isNullOrEmpty(id)) {
      return Result.error("Null/empty ID");
    }
    if (Strings.isNullOrEmpty(name)) {
      return Result.error("Null/empty layer name");
    }
    if (world == null) {
      return Result.error("Null world");
    }
    if (!isEnabled()) {
      return Result.error("BlueMap API not enabled.");
    }

    Optional<BlueMapMap> opt = getMap(world);

    if (opt.isEmpty()) {
      return Result.error("BlueMap does not have the '%s' world", world.getName());
    }

    BlueMapMap mapMap = opt.get();
    MarkerSet existing = mapMap.getMarkerSets().get(id);

    if (existing != null) {
      return Result.error("Layer with ID '%s' is already defined", id);
    }

    MarkerSet set = new MarkerSet(name);
    pluginSets.put(id, new MarkerSetInfo(set, world.getName()));
    mapMap.getMarkerSets().put(id, set);

    return Result.success(new BlueMapLayer(id, set, world, mapMap, this));
  }

  private static String idToPath(String id) {
    return id + (id.endsWith(".png") ? "" : ".png");
  }

  @Override
  public Optional<MapIcon> getIcon(World world, String id) {
    if (Strings.isNullOrEmpty(id)) {
      return Optional.empty();
    }
    if (world == null) {
      return Optional.empty();
    }

    String path = idToPath(id);

    return getMap(world)
        .filter(mapMap -> {
          AssetStorage storage = mapMap.getAssetStorage();

          try {
            return storage.assetExists(path);
          } catch (IOException exc) {
            LOGGER.error("Error checking if asset {} exists", path, exc);
            return false;
          }
        })
        .map(mapMap -> new BlueMapIcon(id, path, mapMap));
  }

  @Override
  public Result<MapIcon> createIcon(World world, String id, String name, InputStream iconData) {
    if (Strings.isNullOrEmpty(id)) {
      return Result.error("Null/empty ID");
    }
    if (Strings.isNullOrEmpty(name)) {
      return Result.error("Null/empty icon name");
    }
    if (iconData == null) {
      return Result.error("Null icon-data");
    }
    if (world == null) {
      return Result.error("Null world");
    }

    Optional<BlueMapMap> mapOpt = getMap(world);

    if (mapOpt.isEmpty()) {
      return Result.error("World '%s' does not have an API equivalent", world.getName());
    }

    BlueMapMap mapMap = mapOpt.get();
    AssetStorage storage = mapMap.getAssetStorage();

    String path = idToPath(id);

    try {
      if (storage.assetExists(path)) {
        return Result.error("Icon with ID '%s' already exists", id);
      }
    } catch (IOException exc) {
      LOGGER.error("Error validating if asset '{}' exists or not. Presuming it doesn't", path, exc);
    }

    try (OutputStream stream = storage.writeAsset(path)) {
      iconData.transferTo(stream);
    } catch (IOException exc) {
      return Result.error("IO error during icon '%s' write: %s", path, exc.getMessage());
    }

    return Result.success(new BlueMapIcon(id, path, mapMap));
  }

  @Override
  public boolean isPlayerVisible(OfflinePlayer player) {
    Objects.requireNonNull(player, "Null player");
    return api().map(a -> a.getWebApp().getPlayerVisibility(player.getUniqueId())).orElse(false);
  }

  @Override
  public void setPlayerVisible(OfflinePlayer player, boolean visible) {
    Objects.requireNonNull(player, "Null player");
    api().ifPresent(api -> api.getWebApp().setPlayerVisibility(player.getUniqueId(), visible));
  }

  @Override
  public boolean isEnabled() {
    return api().isPresent();
  }

  public void save() {
    SerializationHelper.writeJsonFile(markerJson, json -> {
      for (Entry<String, MarkerSetInfo> entry : pluginSets.entrySet()) {
        MarkerSet set = entry.getValue().set();

        if (set == null) {
          return;
        }

        JsonElement serialized = MarkerGson.INSTANCE.toJsonTree(set);
        JsonWrapper markerJson = JsonWrapper.create();
        markerJson.add("world", entry.getValue().world());
        markerJson.add("marker_data", serialized);

        json.add(entry.getKey(), markerJson);
      }
    });
  }

  public void load() {
    SerializationHelper.readAsJson(markerJson, jsonWrapper -> {
      for (Entry<String, JsonElement> entry : jsonWrapper.entrySet()) {
        String id = entry.getKey();
        JsonWrapper json = JsonWrapper.wrap(entry.getValue().getAsJsonObject());
        String worldName = json.getString("world");
        JsonElement data = json.get("marker_data");

        MarkerSet set = MarkerGson.INSTANCE.fromJson(data, MarkerSet.class);

        MarkerSetInfo info = new MarkerSetInfo(set, worldName);
        pluginSets.put(id, info);

        getMap(worldName).ifPresent(blueMapMap -> {
          blueMapMap.getMarkerSets().put(id, set);
        });
      }
    });
  }

  void onDelete(String id) {
    pluginSets.remove(id);
  }

  record MarkerSetInfo(MarkerSet set, String world) {

  }
}
