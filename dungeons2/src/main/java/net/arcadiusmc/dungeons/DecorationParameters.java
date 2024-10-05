package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.Material;

@Getter @Setter
public class DecorationParameters {

  static final Codec<DecorationParameters> CODEC
      = ExistingObjectCodec.<DecorationParameters>create(builder -> {
        builder.optional("max-hanging-light-length", Codec.INT)
            .getter(DecorationParameters::getMaxHangingLightLength)
            .setter(DecorationParameters::setMaxHangingLightLength);

        builder.optional("max-leaf-length", Codec.intRange(1, Integer.MAX_VALUE))
            .getter(DecorationParameters::getMaxLeafLength)
            .setter(DecorationParameters::setMaxLeafLength);

        builder.optional("block-rot", NoiseParameter.CODEC)
            .getter(DecorationParameters::getBlockRot)
            .setter(DecorationParameters::setBlockRot);

        builder.optional("edge-rot", NoiseParameter.CODEC)
            .getter(DecorationParameters::getEdgeRot)
            .setter(DecorationParameters::setEdgeRot);

        builder.optional("puddles", NoiseParameter.CODEC)
            .getter(DecorationParameters::getPuddles)
            .setter(DecorationParameters::setPuddles);

        builder.optional("candles", NoiseParameter.CODEC)
            .getter(DecorationParameters::getCandles)
            .setter(DecorationParameters::setCandles);

        builder.optional("vegetation", NoiseParameter.CODEC)
            .getter(DecorationParameters::getVegetation)
            .setter(DecorationParameters::setVegetation);

        builder.optional("moss", NoiseParameter.CODEC)
            .getter(DecorationParameters::getMoss)
            .setter(DecorationParameters::setMoss);

        builder.optional("bind-candles-to-edges", Codec.BOOL)
            .getter(DecorationParameters::isBindCandleToEdge)
            .setter(DecorationParameters::setBindCandleToEdge);

        builder.optional("leaf-materials", ExtraCodecs.MATERIAL_CODEC.listOf())
            .getter(DecorationParameters::getLeafMaterials)
            .setter(DecorationParameters::setLeafMaterials);

        builder.optional("flora-materials", ExtraCodecs.MATERIAL_CODEC.listOf())
            .getter(DecorationParameters::getFlora)
            .setter(DecorationParameters::setFlora);

        builder.optional("disabled-decorators", Codec.STRING.listOf())
            .getter(DecorationParameters::getDisabledDecorators)
            .setter(DecorationParameters::setDisabledDecorators);

        builder.optional("leaves-as-foliage-rate", Codec.FLOAT)
            .getter(DecorationParameters::getFoliageUsesLeaves)
            .setter(DecorationParameters::setFoliageUsesLeaves);

        builder.optional("foliage-rate", Codec.FLOAT)
            .getter(DecorationParameters::getFoliageRate)
            .setter(DecorationParameters::setFoliageRate);

        builder.optional("vine-berry-rate", Codec.FLOAT)
            .getter(DecorationParameters::getBerryRate)
            .setter(DecorationParameters::setBerryRate);

        builder.optional("waterlog-puddles", Codec.BOOL)
            .getter(DecorationParameters::isWaterlogPuddles)
            .setter(DecorationParameters::setWaterlogPuddles);
      })
      .codec(Codec.unit(DecorationParameters::new));

  private int maxHangingLightLength = 4;
  private int maxLeafLength = 5;

  private float foliageUsesLeaves = 0.1f;
  private float foliageRate = 0.75f;
  private float berryRate = 0.25f;

  private NoiseParameter blockRot = new NoiseParameter();
  private NoiseParameter edgeRot = new NoiseParameter();
  private NoiseParameter puddles = new NoiseParameter();
  private NoiseParameter candles = new NoiseParameter();
  private NoiseParameter vegetation = new NoiseParameter();
  private NoiseParameter moss = new NoiseParameter();

  private boolean bindCandleToEdge = true;
  private boolean waterlogPuddles = false;

  private List<Material> leafMaterials = List.of();
  private List<Material> flora = List.of();
  private List<String> disabledDecorators = List.of();
}
