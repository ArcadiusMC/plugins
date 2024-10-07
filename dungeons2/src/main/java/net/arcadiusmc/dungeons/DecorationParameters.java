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

        builder.optional("leaves", NoiseParameter.CODEC)
            .getter(DecorationParameters::getLeaves)
            .setter(DecorationParameters::setLeaves);

        builder.optional("cave-vines", NoiseParameter.CODEC)
            .getter(DecorationParameters::getCaveVines)
            .setter(DecorationParameters::setCaveVines);

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

        builder.optional("leaves-can-replace", ExtraCodecs.MATERIAL_CODEC.listOf())
            .getter(DecorationParameters::getLeavesCanReplace)
            .setter(DecorationParameters::setLeavesCanReplace);

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

        builder.optional("lit-candle-rate", Codec.FLOAT)
            .getter(DecorationParameters::getLitCandleRate)
            .setter(DecorationParameters::setLitCandleRate);

        builder.optional("waterlog-puddles", Codec.BOOL)
            .getter(DecorationParameters::isWaterlogPuddles)
            .setter(DecorationParameters::setWaterlogPuddles);

        builder.optional("mossy-radius", Codec.INT)
            .getter(DecorationParameters::getMossRadius)
            .setter(DecorationParameters::setMossRadius);

        builder.optional("moss-dropoff-after", Codec.INT)
            .getter(DecorationParameters::getMossDropOffAfter)
            .setter(DecorationParameters::setMossDropOffAfter);

        builder.optional("cave-vine-block", ExtraCodecs.MATERIAL_CODEC)
            .getter(DecorationParameters::getCaveVineBlock)
            .setter(DecorationParameters::setCaveVineBlock);

        builder.optional("cave-vine-bottom-block", ExtraCodecs.MATERIAL_CODEC)
            .getter(DecorationParameters::getCaveVineBottom)
            .setter(DecorationParameters::setCaveVineBottom);

        builder.optional("glow-lichen-instead-of-moss-rate", Codec.FLOAT)
            .getter(DecorationParameters::getGlowLichenInsteadLeavesRate)
            .setter(DecorationParameters::setGlowLichenInsteadLeavesRate);
      })
      .codec(Codec.unit(DecorationParameters::new));

  private int maxHangingLightLength = 4;
  private int maxLeafLength = 5;
  private int mossRadius = 1;
  private int mossDropOffAfter = 1;

  private float foliageUsesLeaves = 0.1f;
  private float foliageRate = 0.75f;
  private float berryRate = 0.25f;
  private float litCandleRate = 0.75f;
  private float glowLichenInsteadLeavesRate = 0.10f;

  private Material caveVineBlock = Material.CAVE_VINES_PLANT;
  private Material caveVineBottom = Material.CAVE_VINES;

  private NoiseParameter blockRot = new NoiseParameter();
  private NoiseParameter edgeRot = new NoiseParameter();
  private NoiseParameter puddles = new NoiseParameter();
  private NoiseParameter candles = new NoiseParameter();
  private NoiseParameter leaves = new NoiseParameter();
  private NoiseParameter caveVines = new NoiseParameter();
  private NoiseParameter moss = new NoiseParameter();

  private boolean bindCandleToEdge = true;
  private boolean waterlogPuddles = false;

  private List<Material> leafMaterials = List.of();
  private List<Material> flora = List.of();
  private List<Material> leavesCanReplace = List.of(Material.GLOW_LICHEN);
  private List<String> disabledDecorators = List.of();
}
