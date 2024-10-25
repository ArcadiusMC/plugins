package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.BROWN_CANDLE;
import static org.bukkit.Material.CANDLE;
import static org.bukkit.Material.GRAVEL;
import static org.bukkit.Material.GRAY_CANDLE;
import static org.bukkit.Material.GREEN_CANDLE;
import static org.bukkit.Material.LIGHT_GRAY_CANDLE;
import static org.bukkit.Material.ORANGE_CANDLE;
import static org.bukkit.Material.WHITE_CANDLE;

import com.mojang.serialization.Codec;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.CandleDecorator.CandleConfig;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.math.Direction;
import org.bukkit.Material;
import org.bukkit.block.data.type.Candle;

public class CandleDecorator extends NoiseDecorator<CandleConfig> implements XyzFunction {

  public static final DecoratorType<CandleDecorator, CandleConfig> TYPE
      = DecoratorType.create(CandleConfig.CODEC, CandleDecorator::new);

  public CandleDecorator(CandleConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.getCandles().isEmpty()) {
      throw new IllegalStateException("No candles set");
    }

    runForEachBlock();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (isSkyAbove(x, y, z) || !isGroundBlock(x, y - 1, z)) {
      return;
    }
    if (!canSupportCandle(x, y - 1, z)) {
      return;
    }

    if (config.isBoundToWall()) {
      Direction edge = edgeDirection(x, y, z);
      Direction wallEdge = wallDirection(x, y, z);

      if (edge == null && wallEdge == null) {
        return;
      }
    }

    if (!testNoise(x, y, z)) {
      return;
    }

    List<Material> candleList = config.getCandles();
    Material candleMaterial = randomFrom(candleList);
    Candle data = (Candle) candleMaterial.createBlockData();

    int candles = randomInt(data.getMinimumCandles(), data.getMaximumCandles() + 1);

    data.setLit(randomBool(config.getLitRate()));
    data.setCandles(candles);

    setBlock(x, y, z, data);
  }

  boolean canSupportCandle(int x, int y, int z) {
    Material material = getBlockType(x, y, z);
    return material != GRAVEL;
  }

  @Getter @Setter
  public static class CandleConfig implements NoiseSettingHolder {

    static final List<Material> DEFAULT_CANDLES = List.of(
        CANDLE,
        WHITE_CANDLE,
        ORANGE_CANDLE,
        GRAY_CANDLE,
        LIGHT_GRAY_CANDLE,
        BROWN_CANDLE,
        GREEN_CANDLE
    );

    static final Codec<CandleConfig> CODEC = ExistingObjectCodec.createCodec(
        CandleConfig::new,
        builder -> {
          builder.optional("noise", NoiseParameter.CODEC)
              .setter(CandleConfig::setNoise)
              .getter(CandleConfig::getNoise);

          builder.optional("candle-blocks", ExtraCodecs.MATERIAL_CODEC.listOf())
              .setter(CandleConfig::setCandles)
              .getter(CandleConfig::getCandles);

          builder.optional("lit-candle-rate", Codec.FLOAT)
              .setter(CandleConfig::setLitRate)
              .getter(CandleConfig::getLitRate);

          builder.optional("bind-to-wall", Codec.BOOL)
              .setter(CandleConfig::setBoundToWall)
              .getter(CandleConfig::isBoundToWall);
        }
    );

    private NoiseParameter noise = new NoiseParameter();
    private List<Material> candles = DEFAULT_CANDLES;
    private float litRate = 0.5f;
    private boolean boundToWall = true;

    @Override
    public NoiseParameter noiseParameters() {
      return noise;
    }
  }
}
