package net.arcadiusmc.dungeons.gen;

import java.util.Random;
import net.arcadiusmc.structure.BlockInfo;
import net.arcadiusmc.structure.BlockProcessor;
import net.arcadiusmc.structure.StructurePlaceConfig;
import org.apache.commons.lang3.mutable.Mutable;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;
import org.bukkit.util.noise.NoiseGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.vector.Vector3i;

public class StairRuinProcessor implements BlockProcessor {

  private final NoiseGenerator noiseGen;
  private final Random random;

  public StairRuinProcessor(NoiseGenerator noiseGen, Random random) {
    this.noiseGen = noiseGen;
    this.random = random;
  }

  @Override
  public @Nullable BlockInfo process(
      @NotNull BlockInfo original,
      @Nullable BlockInfo previous,
      @NotNull StructurePlaceConfig config,
      Mutable<Vector3i> position
  ) {
    if (previous == null) {
      return null;
    }

    BlockData data = previous.getData();
    if (!(data instanceof Stairs stairs)) {
      return previous;
    }
    if (stairs.getShape() != Shape.STRAIGHT || stairs.getHalf() != Half.BOTTOM) {
      return previous;
    }

    Vector3i pos = config.getTransform().apply(position.getValue());

    double noise = noiseGen.noise(pos.x(), pos.y(), pos.z());
    float rnd = random.nextFloat();

    if (rnd >= noise) {
      return previous;
    }

    stairs.setShape(random.nextBoolean() ? Shape.OUTER_LEFT : Shape.OUTER_RIGHT);
    return previous.withData(stairs);
  }
}
