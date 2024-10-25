package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.HangingVineDecorator.HangingVineConfig;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVinesPlant;

public class HangingVineDecorator extends NoiseDecorator<HangingVineConfig> implements XyzFunction {

  public static final DecoratorType<HangingVineDecorator, HangingVineConfig> TYPE
      = DecoratorType.create(HangingVineConfig.CODEC, HangingVineDecorator::new);

  public HangingVineDecorator(HangingVineConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    runForEachBlock();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (isSkyAbove(x, y, z) || isVoidBelow(x, y, z) || !isAir(x, y, z)) {
      return;
    }
    if (!testNoise(x, y, z)) {
      return;
    }
    if (!hasSupport(x, y + 1, z, BlockFace.DOWN)) {
      return;
    }

    caveVineTrail(x, y, z);
  }

  private void caveVineTrail(int x, int y, int z) {
    int freeSpace = freeSpaceDown(x, y, z) / 3;
    int maxLength = config.getMaxLength();

    if (freeSpace < 1) {
      return;
    }

    int length = Math.min(maxLength, randomInt(freeSpace));

    for (int i = 0; i < length; i++) {
      if (!isAir(x, y, z)) {
        break;
      }

      BlockData data;

      if (!isAir(x, y - 1, z) || i == (length - 1)) {
        data = config.getPlantBottomBlock().createBlockData();

        if (data instanceof Ageable ageable) {
          ageable.setAge(ageable.getMaximumAge());
        }
      } else {
        data = config.getPlantBlock().createBlockData();
      }

      if (data instanceof CaveVinesPlant plant) {
        plant.setBerries(randomBool(config.getBerryRate()));
      }

      setBlock(x, y, z, data);
      mossify(x, y, z);

      y--;
    }
  }

  @Getter @Setter
  public static class HangingVineConfig implements NoiseSettingHolder {

    static final Codec<HangingVineConfig> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              NoiseParameter.CODEC.fieldOf("noise")
                  .forGetter(HangingVineConfig::getParameter),

              ExtraCodecs.MATERIAL_CODEC.fieldOf("vine-block")
                  .forGetter(HangingVineConfig::getPlantBlock),

              ExtraCodecs.MATERIAL_CODEC.fieldOf("bottom-block")
                  .forGetter(HangingVineConfig::getPlantBlock),

              Codec.INT.optionalFieldOf("max-length", 4)
                  .forGetter(HangingVineConfig::getMaxLength),

              Codec.FLOAT.optionalFieldOf("berry-rate", .25f)
                  .forGetter(HangingVineConfig::getBerryRate)
          )
          .apply(instance, (noise, block, bottom, maxLength, rate) -> {
            HangingVineConfig cfg = new HangingVineConfig();
            cfg.setParameter(noise);
            cfg.setPlantBlock(block);
            cfg.setPlantBottomBlock(bottom);
            cfg.setMaxLength(maxLength);
            cfg.setBerryRate(rate);
            return cfg;
          });
    });

    private NoiseParameter parameter = new NoiseParameter();
    private Material plantBlock;
    private Material plantBottomBlock;
    private int maxLength;
    private float berryRate;

    @Override
    public NoiseParameter noiseParameters() {
      return parameter;
    }
  }
}
