package net.arcadiusmc.utils.io;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class ConfigCodec {
  private ConfigCodec() {}

  private static final Logger LOGGER = Loggers.getLogger();

  public static final String CONFIG_FILENAME = "config.yml";

  public static <T> Optional<T> loadPluginConfig(JavaPlugin plugin, Codec<T> codec) {
    Path path = plugin.getDataFolder().toPath().resolve(CONFIG_FILENAME);
    PluginJar.saveResources(plugin, CONFIG_FILENAME, path);

    if (!Files.exists(path)) {
      LOGGER.error("Unable to create plugin config file (Default config most likely missing)");
      return Optional.empty();
    }

    try {
      JsonObject json = SerializationHelper.readAsJson(path);

      return codec.parse(JsonOps.INSTANCE, json)
          .mapError(s -> "Failed to load config from file " + path + ": " + s)
          .resultOrPartial(LOGGER::error);
    } catch (Throwable t) {
      LOGGER.error("Error reading plugin config file '{}': {}", path, t.getMessage());
      return Optional.empty();
    }
  }
}
