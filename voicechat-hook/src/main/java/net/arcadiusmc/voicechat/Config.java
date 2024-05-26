package net.arcadiusmc.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.GameEvent;
import org.bukkit.Registry;

public record Config(double sculkThreshold, GameEvent sculkEvent) {

  public static final Config DEFAULT = new Config(110, GameEvent.ENTITY_ACTION);

  public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.DOUBLE.optionalFieldOf("sculk-activation-volume-db", DEFAULT.sculkThreshold)
                .forGetter(Config::sculkThreshold),

            ExtraCodecs.registryCodec(Registry.GAME_EVENT)
                .optionalFieldOf("sculk-activation-gameevent", DEFAULT.sculkEvent)
                .forGetter(Config::sculkEvent)
        )

        .apply(instance, Config::new);
  });
}
