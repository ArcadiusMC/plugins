package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.block.BlockFace;

public record FacingPosition(int x, int y, int z, BlockFace facing) {

  static final Codec<FacingPosition> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(FacingPosition::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(FacingPosition::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(FacingPosition::z),

            ExtraCodecs.enumCodec(BlockFace.class)
                .optionalFieldOf("facing", BlockFace.NORTH)
                .forGetter(FacingPosition::facing)
        )
        .apply(instance, FacingPosition::new);
  });
}
