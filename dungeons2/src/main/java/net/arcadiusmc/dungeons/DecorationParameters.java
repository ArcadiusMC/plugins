package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;

@Getter @Setter
public class DecorationParameters {

  static final Codec<DecorationParameters> CODEC
      = ExistingObjectCodec.<DecorationParameters>create(builder -> {
        builder.optional("block-rot-frequency", Codec.FLOAT)
            .getter(DecorationParameters::getBlockRotFrequency)
            .setter(DecorationParameters::setBlockRotFrequency);

        builder.optional("edge-rot-frequency", Codec.FLOAT)
            .getter(DecorationParameters::getEdgeRotFrequency)
            .setter(DecorationParameters::setEdgeRotFrequency);

        builder.optional("max-hanging-light-length", Codec.INT)
            .getter(DecorationParameters::getMaxHangingLightLength)
            .setter(DecorationParameters::setMaxHangingLightLength);

        builder.optional("max-leaf-length", Codec.intRange(1, Integer.MAX_VALUE))
            .getter(DecorationParameters::getMaxLeafLength)
            .setter(DecorationParameters::setMaxLeafLength);

        builder.optional("puddle-frequency", Codec.FLOAT)
            .getter(DecorationParameters::getPuddleFrequency)
            .setter(DecorationParameters::setPuddleFrequency);

        builder.optional("puddle-noise-gate", Codec.FLOAT)
            .getter(DecorationParameters::getPuddleNoiseGate)
            .setter(DecorationParameters::setPuddleNoiseGate);

        builder.optional("candle-frequency", Codec.FLOAT)
            .getter(DecorationParameters::getCandleFrequency)
            .setter(DecorationParameters::setCandleFrequency);

        builder.optional("candle-noise-gate", Codec.FLOAT)
            .getter(DecorationParameters::getCandleNoiseGate)
            .setter(DecorationParameters::setCandleNoiseGate);

        builder.optional("bind-candle-to-edge", Codec.BOOL)
            .getter(DecorationParameters::isBindCandleToEdge)
            .setter(DecorationParameters::setBindCandleToEdge);
      })
      .codec(Codec.unit(DecorationParameters::new));

  private int maxHangingLightLength = 4;
  private int maxLeafLength = 5;

  private float blockRotFrequency = 0.08f;
  private float edgeRotFrequency = 0.08f;

  private float puddleFrequency = 0.08f;
  private float puddleNoiseGate = 0.65f;

  private float candleFrequency = 0.08f;
  private float candleNoiseGate = 0.65f;
  private boolean bindCandleToEdge = true;
}
