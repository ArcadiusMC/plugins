package net.arcadiusmc.cosmetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CosmeticsConfig(
    int deathEffectsPrice,
    int arrowEffectsPrice,
    int travelEffectsPrice
) {

  static final CosmeticsConfig DEFAULT = new CosmeticsConfig(2000, 1000, 3000);

  static final Codec<CosmeticsConfig> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("death-effect-price", DEFAULT.deathEffectsPrice)
                .forGetter(CosmeticsConfig::deathEffectsPrice),
            Codec.INT.optionalFieldOf("arrow-effect-price", DEFAULT.arrowEffectsPrice)
                .forGetter(CosmeticsConfig::arrowEffectsPrice),
            Codec.INT.optionalFieldOf("travel-effect-price", DEFAULT.travelEffectsPrice)
                .forGetter(CosmeticsConfig::travelEffectsPrice)
        )
        .apply(instance, CosmeticsConfig::new);
  });
}
