package net.arcadiusmc.structure.pool;

import static net.arcadiusmc.structure.BlockStructure.DEFAULT_PALETTE_NAME;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.utils.io.Results;

public record PoolEntry(int weight, String structureName, String paletteName) {

  public static final int DEFAULT_WEIGHT = 1;

  public static Pattern SPLIT_PATTERN = Pattern.compile(
      "(" + Registries.VALID_KEY_REGEX + ")"
          + "(?:::(" + Registries.VALID_KEY_REGEX + "))?"
  );

  private static final Codec<PoolEntry> STRING_CODEC = Codec.STRING.comapFlatMap(
      string -> {
        Matcher matcher = SPLIT_PATTERN.matcher(string);
        if (!matcher.matches()) {
          return Results.error("Invalid structure reference: '%s'", string);
        }

        String struct = matcher.group(1);
        String palette = Strings.nullToEmpty(matcher.group(2));

        return Results.success(new PoolEntry(DEFAULT_WEIGHT, struct, palette));
      },
      poolEntry -> {
        if (Strings.isNullOrEmpty(poolEntry.paletteName)) {
          return poolEntry.structureName;
        }

        return poolEntry.structureName + "::" + poolEntry.paletteName;
      }
  );

  private static final Codec<PoolEntry> RECORD_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("weight", DEFAULT_WEIGHT)
                .forGetter(o -> o.weight),

            Codec.STRING.fieldOf("structure")
                .forGetter(o -> o.structureName),

            Codec.STRING.optionalFieldOf("palette")
                .forGetter(o -> {
                  String str = o.paletteName;

                  if (Strings.isNullOrEmpty(str) || str.equals(DEFAULT_PALETTE_NAME)) {
                    return Optional.empty();
                  }

                  return Optional.of(str);
                })
        )
        .apply(instance, (weight, struct, palette) -> {
          return new PoolEntry(weight, struct, palette.orElse(""));
        });
  });

  public static final Codec<PoolEntry> CODEC = Codec.either(STRING_CODEC, RECORD_CODEC)
      .xmap(
          e -> e.map(Function.identity(), Function.identity()),
          poolEntry -> {
            if (poolEntry.isSimple()) {
              return Either.left(poolEntry);
            }
            return Either.right(poolEntry);
          }
      );

  public boolean isSimple() {
    return weight == DEFAULT_WEIGHT;
  }
}
