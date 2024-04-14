package net.arcadiusmc.titles;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

public class ActiveTitleMap {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<Map<UUID, String>> MAP_CODEC =
      Codec.unboundedMap(ExtraCodecs.STRING_UUID, ExtraCodecs.KEY_CODEC);

  private final Map<UUID, String> titleMap = new HashMap<>();

  private final Path path;

  public ActiveTitleMap(Path path) {
    this.path = path;
  }

  public void save() {
    MAP_CODEC.encodeStart(JsonOps.INSTANCE, titleMap)
        .mapError(s -> "Failed to save title map: " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(jsonElement -> {
          SerializationHelper.writeJson(path, jsonElement);
        });
  }

  public void load() {
    titleMap.clear();

    SerializationHelper.readAsJson(path, object -> {
      MAP_CODEC.parse(JsonOps.INSTANCE, object)
          .mapError(s -> "Failed load title map: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(titleMap::putAll);
    });
  }

  public Optional<Holder<Title>> getTitle(UUID playerId) {
    Objects.requireNonNull(playerId, "Null playerId");

    String titleKey = titleMap.get(playerId);

    if (Strings.isNullOrEmpty(titleKey)) {
      return Optional.empty();
    }

    return Titles.REGISTRY.getHolder(titleKey);
  }

  public void setTitle(UUID playerId, Holder<Title> holder) {
    Objects.requireNonNull(playerId, "Null playerId");
    Objects.requireNonNull(holder, "Null holder");
    titleMap.put(playerId, holder.getKey());
  }
}
