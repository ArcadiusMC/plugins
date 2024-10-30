package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;

@Getter @Setter
public class DecorationParameters {

  static final Codec<DecorationParameters> CODEC
      = ExistingObjectCodec.<DecorationParameters>create(builder -> {
        builder.optional("block-rot", NoiseParameter.CODEC)
            .getter(DecorationParameters::getBlockRot)
            .setter(DecorationParameters::setBlockRot);

        builder.optional("mossy-radius", Codec.INT)
            .getter(DecorationParameters::getMossRadius)
            .setter(DecorationParameters::setMossRadius);

        builder.optional("moss-dropoff-after", Codec.INT)
            .getter(DecorationParameters::getMossDropOffAfter)
            .setter(DecorationParameters::setMossDropOffAfter);

        builder.optional("glow-lichen-instead-of-moss-rate", Codec.FLOAT)
            .getter(DecorationParameters::getGlowLichenInsteadLeavesRate)
            .setter(DecorationParameters::setGlowLichenInsteadLeavesRate);

        builder.optional("disabled-passes", Codec.STRING.listOf())
            .getter(DecorationParameters::getDisabledPasses)
            .setter(DecorationParameters::setDisabledPasses);
      })
      .codec(Codec.unit(DecorationParameters::new));

  private int mossRadius = 1;
  private int mossDropOffAfter = 1;

  private float glowLichenInsteadLeavesRate = 0.10f;
  private NoiseParameter blockRot = new NoiseParameter();

  private List<String> disabledPasses = List.of();
}
