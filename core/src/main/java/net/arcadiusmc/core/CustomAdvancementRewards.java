package net.arcadiusmc.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.NamespacedKey;
import org.slf4j.Logger;

@Getter
public class CustomAdvancementRewards {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<Map<NamespacedKey, List<String>>> CODEC
      = Codec.unboundedMap(ExtraCodecs.NAMESPACED_KEY, Codec.STRING.listOf());

  private Map<NamespacedKey, List<String>> rewardMap = Object2ObjectMaps.emptyMap();
  private final Path path;

  public CustomAdvancementRewards(CorePlugin plugin) {
    this.path = plugin.getDataPath().resolve("custom-advancement-rewards.yml");
  }

  public void load() {
    rewardMap = Object2ObjectMaps.emptyMap();
    PluginJar.saveResources("custom-advancement-rewards.yml");

    SerializationHelper.readAsJson(path, json -> {
      CODEC.parse(JsonOps.INSTANCE, json)
          .mapError(s -> "Failed to load custom advancement rewards: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(map -> {
            rewardMap = map;
          });
    });
  }
}
