package net.arcadiusmc.discord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record BoostCommands(List<String> boostingBegin, List<String> boostingEnd) {
  static final BoostCommands EMPTY = new BoostCommands(List.of(), List.of());

  static final Codec<BoostCommands> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.listOf().optionalFieldOf("on-boosting-begin", List.of())
                .forGetter(o -> o.boostingBegin),

            Codec.STRING.listOf().optionalFieldOf("on-boosting-end", List.of())
                .forGetter(o -> o.boostingEnd)
        )
        .apply(instance, BoostCommands::new);
  });
}
