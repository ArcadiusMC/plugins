package net.arcadiusmc.dungeons;

import com.google.common.base.Strings;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

@Getter @Setter
public class LevelTypeManager {

  private static final Logger LOGGER = Loggers.getLogger();

  static final String DIR_NAME = "level-types";
  static final String REGULAR = "regular";

  private final Registry<LevelType> registry = Registries.newRegistry();
  private final Path directory;

  public LevelTypeManager(DungeonsPlugin plugin) {
    this.directory = plugin.getDataPath().resolve(DIR_NAME);
    PluginJar.saveResources(DIR_NAME, this.directory);
  }

  public void load() {
    registry.clear();

    PathUtil.iterateDirectory(directory, true, true, path -> {
      String key = PathUtil.getFileKey(directory, path);

      SerializationHelper.readAsJson(path, jsonObject -> {
        LevelType.CODEC.parse(JsonOps.INSTANCE, jsonObject)
            .mapError(s -> "Failed to load level type " + key + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(type -> {
              if (Strings.isNullOrEmpty(type.getName())) {
                type.setName(key);
              }

              registry.register(key, type);
            });
      });
    });

    LOGGER.info("Loaded {} level types", registry.size());
  }
}
