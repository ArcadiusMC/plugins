package net.arcadiusmc.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.ChatEmotes;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class EmojiLoader {

  private static final Logger LOGGER = Loggers.getLogger();

  private static Codec<Map<String, Component>> MAP_CODEC
      = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, ExtraCodecs.COMPONENT);

  private final Set<String> loadedNames = new HashSet<>();

  public void load() {
    Path file = PathUtil.pluginPath("chat_emojis.toml");
    PluginJar.saveResources("chat_emojis.toml", file);

    if (!loadedNames.isEmpty()) {
      ChatEmotes.unregisterAll(loadedNames);
      loadedNames.clear();
    }

    SerializationHelper.readAsJson(file, json -> {
      MAP_CODEC.parse(JsonOps.INSTANCE, json.getSource())
          .mapError(s -> "Failed to load emojis: " + s)
          .resultOrPartial(LOGGER::error)

          .ifPresent(map -> {
            map.forEach((s, component) -> {
              if (ChatEmotes.TOKEN_2_EMOTE.containsKey(s)) {
                LOGGER.error("Cannot register chat emote '{}', cannot override default", s);
                return;
              }

              ChatEmotes.register(s, component);
              loadedNames.add(s);
            });
          });
    });
  }
}
