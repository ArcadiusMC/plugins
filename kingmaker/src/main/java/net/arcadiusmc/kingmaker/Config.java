package net.arcadiusmc.kingmaker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Config(
    String monarchPermissionGroup
) {

  static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("monarch-permission-group")
                .forGetter(Config::monarchPermissionGroup)
        )
        .apply(instance, Config::new);
  });
}
