package net.arcadiusmc.dungeons.gen;

import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_ALL;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NONE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NW;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_SE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_SW;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.createStairs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.BlockIterations.BlockIteration;
import net.arcadiusmc.dungeons.gen.PuddleDecorator.PuddleConfig;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

public class PuddleDecorator extends NoiseDecorator<PuddleConfig> implements XyzFunction {

  public static final DecoratorType<PuddleDecorator, PuddleConfig> TYPE
      = DecoratorType.create(PuddleConfig.CODEC, PuddleDecorator::new);

  public PuddleDecorator(PuddleConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    runForEachBlock();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (!isPuddleBlock(x, y, z) || isSkyAbove(x, y, z)) {
      return;
    }

    Material mat = getBlockType(x, y, z);
    if (mat == null) {
      return;
    }

    BlockIteration iteration = BlockIterations.getIteration(mat);
    if (iteration == null) {
      return;
    }

    if (iteration.getBlock() != mat) {
      return;
    }

    int mask = sampleMask(x, y, z);

    if (mask == Q_NONE) {
      return;
    }
    if (mask == Q_ALL) {
      setPuddleBlock(x, y, z, iteration.getSlab().createBlockData());
      return;
    }

    BlockData data = createStairs(iteration.getStairs(), mask);
    if (data == null) {
      data = iteration.getSlab().createBlockData();
    }

    setPuddleBlock(x, y, z, data);
  }

  void setPuddleBlock(int x, int y, int z, BlockData data) {
    boolean waterlogged = false;

    if (data instanceof Waterlogged logged) {
      waterlogged = config.isWaterlogged();
      logged.setWaterlogged(waterlogged);
    }

    setBlock(x, y, z, data);

    if (waterlogged) {
      mossify(x, y, z);
    }
  }

  int sampleMask(int x, int y, int z) {
    double minX = x + 0.25;
    double minZ = z + 0.25;
    double maxX = minX + 0.5;
    double maxZ = minZ + 0.5;
    double ny = y + 0.75d;

    int mask = blockQuadrantsMask(
        getNoise(minX, ny, minZ),
        getNoise(maxX, ny, minZ),
        getNoise(minX, ny, maxZ),
        getNoise(maxX, ny, maxZ)
    );

    boolean westAir = isAir(x - 1, y, z /*, BlockFace.WEST*/);
    boolean eastAir = isAir(x + 1, y, z /*, BlockFace.EAST*/);
    boolean northAir = isAir(x, y, z - 1 /*, BlockFace.NORTH*/);
    boolean southAir = isAir(x, y, z + 1 /*, BlockFace.SOUTH*/);

    if (westAir | northAir) {
      mask &= ~Q_NW;
    }
    if (westAir | southAir) {
      mask &= ~Q_SW;
    }
    if (eastAir | northAir) {
      mask &= ~Q_NE;
    }
    if (eastAir | southAir) {
      mask &= ~Q_SE;
    }

    return mask;
  }

  int blockQuadrantsMask(double nw, double ne, double sw, double se) {
    return getMask(Q_NW, nw)
        | getMask(Q_NE, ne)
        | getMask(Q_SW, sw)
        | getMask(Q_SE, se);
  }

  int getMask(int mask, double noise) {
    if (!testNoise(noise)) {
      return 0;
    }
    return mask;
  }

  boolean isPuddleBlock(int x, int y, int z) {
    if (!isGroundBlock(x, y, z)) {
      return false;
    }

    if (isSkyAbove(x, y, z)) {
      return false;
    }

    Material blockType = getBlockType(x, y, z);
    if (blockType != null) {
      BlockIteration iter = BlockIterations.getIteration(blockType);

      if (iter == null) {
        return false;
      }
    }

    return true;
  }

  @Getter @Setter
  public static class PuddleConfig implements NoiseSettingHolder {

    static final Codec<PuddleConfig> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              NoiseParameter.CODEC.fieldOf("noise")
                  .forGetter(PuddleConfig::getNoise),

              Codec.BOOL.optionalFieldOf("waterlogged", false)
                  .forGetter(PuddleConfig::isWaterlogged)
          )
          .apply(instance, (noise, waterlogged) -> {
            PuddleConfig config = new PuddleConfig();
            config.setNoise(noise);
            config.setWaterlogged(waterlogged);
            return config;
          });
    });

    private NoiseParameter noise;
    private boolean waterlogged = false;

    @Override
    public NoiseParameter noiseParameters() {
      return noise;
    }
  }
}
