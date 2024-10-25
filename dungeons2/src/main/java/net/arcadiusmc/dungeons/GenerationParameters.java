package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.apache.commons.lang3.Range;

@Getter @Setter
public class GenerationParameters {

  public static final Codec<Range<Integer>> INT_RANGE_CODEC = ExtraCodecs.combine(
      RecordCodecBuilder.create(instance -> {
        return instance
            .group(
                Codec.intRange(0, Integer.MAX_VALUE)
                    .optionalFieldOf("min", Integer.MIN_VALUE)
                    .forGetter(Range::getMinimum),
                Codec.intRange(0, Integer.MAX_VALUE)
                    .optionalFieldOf("max", Integer.MIN_VALUE)
                    .forGetter(Range::getMinimum)
            )
            .apply(instance, Range::between);
      }),
      Codec.INT.xmap(Range::is, Range::getMinimum)
  );

  static final Codec<GenerationParameters> CODEC
      = ExistingObjectCodec.<GenerationParameters>create(builder -> {
        builder.optional("depth-range", INT_RANGE_CODEC)
            .getter(GenerationParameters::getDepthRange)
            .setter(GenerationParameters::setDepthRange);

        builder.optional("room-depth-range", INT_RANGE_CODEC)
            .getter(GenerationParameters::getRoomDepth)
            .setter(GenerationParameters::setRoomDepth);

        builder.optional("connector-depth-range", INT_RANGE_CODEC)
            .getter(GenerationParameters::getConnectorDepth)
            .setter(GenerationParameters::setConnectorDepth);

        builder.optional("max-room-exits", Codec.INT)
            .getter(GenerationParameters::getMaxRoomExits)
            .setter(GenerationParameters::setMaxRoomExits);

        builder.optional("max-connector-exits", Codec.INT)
            .getter(GenerationParameters::getMaxConnectorExits)
            .setter(GenerationParameters::setMaxConnectorExits);

        builder.optional("required-rooms", Codec.INT)
            .getter(GenerationParameters::getRequiredRooms)
            .setter(GenerationParameters::setRequiredRooms);

        builder.optional("room-open-chance", Codec.FLOAT)
            .getter(GenerationParameters::getRoomOpenChance)
            .setter(GenerationParameters::setRoomOpenChance);

        builder.optional("decorated-gate-chance", Codec.FLOAT)
            .getter(GenerationParameters::getDecoratedGateChance)
            .setter(GenerationParameters::setDecoratedGateChance);
      })
      .codec(Codec.unit(GenerationParameters::new));

  private Range<Integer> depthRange = Range.between(5, 10);
  private Range<Integer> roomDepth = Range.between(1, 1);
  private Range<Integer> connectorDepth = Range.between(1, 2);

  private int maxRoomExits = 3;
  private int maxConnectorExits = 4;
  private int requiredRooms = 3;

  private float roomOpenChance = 0.1f;
  private float decoratedGateChance = 0.5f;
}
