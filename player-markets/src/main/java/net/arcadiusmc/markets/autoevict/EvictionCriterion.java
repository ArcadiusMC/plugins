package net.arcadiusmc.markets.autoevict;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.registry.Holder;

public record EvictionCriterion<V>(
    float aggressionThreshold,
    int persistence,
    V value,
    Holder<CriterionType<V>> type
) {

  static <V> Codec<EvictionCriterion<V>> codec(Holder<CriterionType<V>> type) {
    Codec<V> valueCodec = type.getValue().getValueCodec();

    return RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.FLOAT.optionalFieldOf("aggression-threshold", 1.1f)
                  .forGetter(o -> o.aggressionThreshold),

              Codec.INT.optionalFieldOf("persistence", 1)
                  .forGetter(o -> o.persistence),

              valueCodec.fieldOf("base")
                  .forGetter(o -> o.value)
          )
          .apply(instance, (aggressionThreshold, persistence, v) -> {
            return new EvictionCriterion<>(aggressionThreshold, persistence, v, type);
          });
    });
  }

}
