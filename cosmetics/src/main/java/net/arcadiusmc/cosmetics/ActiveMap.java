package net.arcadiusmc.cosmetics;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ActiveMap {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<Map<UUID, Map<String, String>>> CODEC = Codec.unboundedMap(
      ExtraCodecs.STRING_UUID,
      Codec.unboundedMap(ExtraCodecs.KEY_CODEC, ExtraCodecs.KEY_CODEC)
  );

  private final Map<UUID, Map<String, String>> activeCosmeticMap
      = new Object2ObjectOpenHashMap<>();

  private final Path path;
  private boolean dirty = false;

  public ActiveMap(Path path) {
    this.path = path;
  }

  public <T> void setActive(User user, CosmeticType<T> type, Cosmetic<T> cosmetic) {
    Objects.requireNonNull(user, "Null user");
    Objects.requireNonNull(type, "Null type");

    Map<String, String> activeMap = activeCosmeticMap.get(user.getUniqueId());

    if (activeMap == null) {
      if (cosmetic == null) {
        return;
      }

      activeMap = new Object2ObjectOpenHashMap<>();
      activeCosmeticMap.put(user.getUniqueId(), activeMap);
    }

    if (cosmetic == null) {
      dirty |= activeMap.remove(type.getKey()) != null;
    } else {
      String key = cosmetic.getKey();
      dirty |= !Objects.equals(activeMap.put(type.getKey(), key), key);
    }
  }

  public @Nullable <T> Cosmetic<T> getActive(UUID playerId, CosmeticType<T> type) {
    Objects.requireNonNull(playerId, "Null playerId");
    Objects.requireNonNull(type, "Null type");

    Map<String, String> map = activeCosmeticMap.get(playerId);
    if (map == null) {
      return null;
    }

    String cosmeticKey = map.get(type.getKey());
    if (Strings.isNullOrEmpty(cosmeticKey)) {
      return null;
    }

    return type.getCosmetics().get(cosmeticKey);
  }

  public void save() {
    if (!dirty) {
      return;
    }

    CODEC.encodeStart(JsonOps.INSTANCE, activeCosmeticMap)
        .mapError(s -> "Failed to encode active cosmetics map: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(element -> SerializationHelper.writeJson(path, element));
  }

  public void load() {
    activeCosmeticMap.clear();
    dirty = false;

    SerializationHelper.readJson(path)
        .flatMap(jsonObject -> CODEC.parse(JsonOps.INSTANCE, jsonObject))
        .mapError(s -> "Failed to load active cosmetic map: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(playerIdMap -> {
          playerIdMap.forEach((uuid, stringStringMap) -> {
            Map<String, String> map = new Object2ObjectOpenHashMap<>(stringStringMap);
            activeCosmeticMap.put(uuid, map);
          });
        });
  }
}
