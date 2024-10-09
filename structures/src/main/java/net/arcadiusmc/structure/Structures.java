package net.arcadiusmc.structure;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.structure.pool.StructurePool;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

public final class Structures {

  static final String FILE_FORMAT = ".dat";

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Registry<BlockStructure> registry = Registries.newRegistry();

  @Getter
  private final Registry<StructurePool> poolRegistry = Registries.newRegistry();

  @Getter
  private final Path directory;
  private final Path pools;

  private boolean deleteRemovedStructures = true;

  public Structures(StructuresPlugin plugin) {
    this.directory = PathUtil.pluginPath(plugin, "structures");
    this.pools = PathUtil.pluginPath(plugin, "pools");

    PathUtil.ensureDirectoryExists(pools);

    registry.setListener(RegistryListener.removalListener(holder -> {
      if (!deleteRemovedStructures) {
        return;
      }

      delete(holder);
    }));
  }

  public void save() {
    for (var structure : registry.entries()) {
      Path p = getPath(structure);
      SerializationHelper.writeTagFile(p, structure.getValue()::save);
    }
  }

  public void load() {
    deleteRemovedStructures = false;
    registry.clear();
    poolRegistry.clear();
    deleteRemovedStructures = true;

    if (!Files.exists(directory)) {
      return;
    }

    PathUtil.findAllFiles(directory, true).forEach(fpath -> {
      if (fpath.startsWith("pools") || !fpath.endsWith(FILE_FORMAT)) {
        return;
      }

      Path path = directory.resolve(fpath);
      BlockStructure structure = new BlockStructure();

      LOGGER.debug("loading structure '{}'", fpath);

      if (!SerializationHelper.readTagFile(path, structure::load)) {
        LOGGER.warn("Couldn't load '{}'", fpath);
        return;
      }

      String key = fpath.substring(0, fpath.length() - FILE_FORMAT.length());
      registry.register(key, structure);
    });

    PathUtil.iterateDirectory(pools, true, true, path -> {
      String key = PathUtil.getFileKey(pools, path);
      JsonElement element;

      try {
        element = JsonUtils.readFile(path);
      } catch (IOException exc) {
        LOGGER.error("Failed to read structure pool file {}", path, exc);
        return;
      }

      if (!element.isJsonArray()) {
        LOGGER.error(
            "Cannot read structure pool {} (path={}), file is not a JSON array",
            key, path
        );

        return;
      }

      StructurePool.CODEC.parse(JsonOps.INSTANCE, element)
          .mapError(string -> "Failed to load structure pool '" + key + "': " + string)
          .resultOrPartial(LOGGER::error)
          .ifPresent(pool -> poolRegistry.register(key, pool));
    });
  }

  public void delete(Holder<BlockStructure> structure) {
    Path path = getPath(structure);

    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      LOGGER.error("Error deleting structure {} at {}", structure.getKey(), path, e);
    }
  }

  private Path getPath(Holder<BlockStructure> holder) {
    String strPath = holder.getKey();
    return directory.resolve(strPath + FILE_FORMAT);
  }
}