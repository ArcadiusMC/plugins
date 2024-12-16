package net.arcadiusmc.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.papermc.paper.advancement.AdvancementDisplay;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.slf4j.Logger;

@Getter
public class CustomAdvancementRewards {

  private static final Logger LOGGER = Loggers.getLogger();

  public static NamespacedKey ANY = NamespacedKey.minecraft("any");
  public static NamespacedKey ANY_GOAL = NamespacedKey.minecraft("any_goal");
  public static NamespacedKey ANY_CHALLENGE = NamespacedKey.minecraft("any_challenge");
  public static NamespacedKey ANY_TASK = NamespacedKey.minecraft("any_task");

  static final Codec<Map<NamespacedKey, List<String>>> CODEC
      = Codec.unboundedMap(ExtraCodecs.NAMESPACED_KEY, Codec.STRING.listOf());

  private Map<NamespacedKey, List<String>> rewardMap = Object2ObjectMaps.emptyMap();
  private final Path path;

  public CustomAdvancementRewards(CorePlugin plugin) {
    this.path = plugin.getDataPath().resolve("custom-advancement-rewards.yml");
  }

  public List<String> getCommands(Advancement advancement) {
    List<String> res = new ArrayList<>(getCommands(advancement.getKey()));

    AdvancementDisplay display = advancement.getDisplay();
    if (display != null) {
      NamespacedKey extraKey = switch (display.frame()) {
        case GOAL -> ANY_GOAL;
        case TASK -> ANY_TASK;
        case CHALLENGE -> ANY_CHALLENGE;
      };
      res.addAll(getCommands(extraKey));
    }

    return res;
  }

  public List<String> getCommands(NamespacedKey key) {
    if (rewardMap == null) {
      return ObjectLists.emptyList();
    }

    return rewardMap.getOrDefault(key, ObjectLists.emptyList());
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
