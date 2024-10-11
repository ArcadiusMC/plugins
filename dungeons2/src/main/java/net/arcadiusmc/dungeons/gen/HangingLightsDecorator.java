package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.CHAIN;
import static org.bukkit.Material.LANTERN;
import static org.bukkit.Material.SOUL_LANTERN;
import static org.bukkit.Material.STONE_BRICK_WALL;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.gen.HangingLightsDecorator.HangingLightsConfig;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.type.Chain;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;
import org.spongepowered.math.vector.Vector3i;

public class HangingLightsDecorator extends Decorator<HangingLightsConfig> {

  static final DecoratorType<HangingLightsDecorator, HangingLightsConfig> TYPE
      = DecoratorType.create(HangingLightsConfig.CODEC, HangingLightsDecorator::new);

  public HangingLightsDecorator(HangingLightsConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    List<GeneratorFunction> functionList = getFunctions(LevelFunctions.HANGING_LIGHT);
    processList(functionList);
  }

  private void processList(List<GeneratorFunction> functionList) {
    for (GeneratorFunction func : functionList) {
      CompoundTag data = func.getData();
      boolean soul = data.getBoolean("soul", false);

      float spawnChance = data.getFloat("spawn_chance", 1f);
      if (spawnChance < 1f) {
        float rnd = random.nextFloat();

        if (rnd >= spawnChance) {
          continue;
        }
      }

      generateHangingLight(func.getPosition(), soul);
    }
  }

  private void generateHangingLight(Vector3i originPoint, boolean soul) {
    int x = originPoint.x();
    int y = originPoint.y();
    int z = originPoint.z();

    int freeSpace = freeSpaceDown(x, y, z);
    int fourth = freeSpace / 4;

    if (fourth < 1) {
      return;
    }

    int maxLen = config.maxLength;
    int chainLen = Math.min(maxLen, random.nextInt(fourth));

    int by = y;

    if (isAir(x, by + 1, z)) {
      addHangingTop(x, by + 1, z);
    }

    BlockData chainData = createChainData();

    for (int i = 0; i < chainLen; i++) {
      setBlock(x, by, z, chainData);
      by--;
    }

    Material material = soul ? config.soulLantern : config.lantern;
    BlockData data = material.createBlockData();

    if (data instanceof Hangable lantern) {
      lantern.setHanging(true);
    }

    setBlock(x, by, z, data);
  }

  private void addHangingTop(int x, int y, int z) {
    Wall wall = (Wall) STONE_BRICK_WALL.createBlockData();

    if (hasSupport(x, y, z + 1, BlockFace.NORTH)) {
      wall.setHeight(BlockFace.SOUTH, Height.LOW);
    }
    if (hasSupport(x, y, z - 1, BlockFace.SOUTH)) {
      wall.setHeight(BlockFace.NORTH, Height.LOW);
    }
    if (hasSupport(x + 1, y, z, BlockFace.WEST)) {
      wall.setHeight(BlockFace.EAST, Height.LOW);
    }
    if (hasSupport(x + 1, y, z, BlockFace.EAST)) {
      wall.setHeight(BlockFace.WEST, Height.LOW);
    }

    setBlock(x, y, z, wall);
  }

  private BlockData createChainData() {
    Chain data = (Chain) CHAIN.createBlockData();
    data.setAxis(Axis.Y);
    return data;
  }

  public record HangingLightsConfig(
      int maxLength,
      Material lantern,
      Material soulLantern
  ) {
    static final Codec<HangingLightsConfig> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.INT.optionalFieldOf("max-length", 4)
                  .forGetter(HangingLightsConfig::maxLength),

              ExtraCodecs.MATERIAL_CODEC.optionalFieldOf("lantern-block", LANTERN)
                  .forGetter(HangingLightsConfig::lantern),

              ExtraCodecs.MATERIAL_CODEC.optionalFieldOf("soul-lantern-block", SOUL_LANTERN)
                  .forGetter(HangingLightsConfig::lantern)
          )
          .apply(instance, HangingLightsConfig::new);
    });
  }
}
