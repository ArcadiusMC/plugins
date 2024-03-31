package net.arcadiusmc.markets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Duration;
import java.util.List;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;

public record MarketsConfig(
    int baseRent,
    int defaultPrice,
    Duration rentInterval,
    Duration taxResetInterval,
    Duration actionCooldown,
    Duration marketTickInterval,
    List<TaxBracket> taxBrackets
) {
  public static final MarketsConfig EMPTY = new MarketsConfig(
      200,
      50_000,
      Duration.ofDays(7),
      Duration.ofDays(28),
      Duration.ofDays(1),
      Duration.ofMinutes(5),
      java.util.List.of()
  );

  public static final Codec<MarketsConfig> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("base-rent", EMPTY.baseRent)
                .forGetter(o -> o.baseRent),

            Codec.INT.optionalFieldOf("default-price", EMPTY.defaultPrice)
                .forGetter(o -> o.defaultPrice),

            ExtraCodecs.DURATION.optionalFieldOf("rent-interval", EMPTY.rentInterval)
                .forGetter(o -> o.rentInterval),

            ExtraCodecs.DURATION.optionalFieldOf("tax-reset-interval", EMPTY.taxResetInterval)
                .forGetter(o -> o.taxResetInterval),

            ExtraCodecs.DURATION.optionalFieldOf("action-cooldown", EMPTY.actionCooldown)
                .forGetter(o -> o.actionCooldown),

            ExtraCodecs.DURATION.optionalFieldOf("market-tick-interval", EMPTY.marketTickInterval)
                .forGetter(o -> o.marketTickInterval),

            TaxBracket.CODEC.listOf().optionalFieldOf("tax-brackets", java.util.List.of())
                .forGetter(o -> o.taxBrackets)
        )
        .apply(instance, MarketsConfig::new);
  });


  public record TaxBracket(float rate, IntRange earningsRange) {
    public static final Codec<IntRange> RANGE_CODEC = Codec.STRING
        .comapFlatMap(s -> ExtraCodecs.safeParse(s, ArgumentTypes.intRange()), IntRange::toString);

    public static final Codec<TaxBracket> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.FLOAT.fieldOf("tax-rate").forGetter(o -> o.rate),
              RANGE_CODEC.fieldOf("earnings-range").forGetter(o -> o.earningsRange)
          )
          .apply(instance, TaxBracket::new);
    });
  }
}
