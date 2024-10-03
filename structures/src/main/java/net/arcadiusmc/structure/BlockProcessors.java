package net.arcadiusmc.structure;

import java.util.Random;
import net.arcadiusmc.structure.BlockRotProcessor.IntegrityProvider;
import net.arcadiusmc.utils.math.Transform;
import org.apache.commons.lang3.mutable.Mutable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.vector.Vector3i;

/**
 * Utility class for methods and constants related to {@link BlockProcessor}s
 */
public final class BlockProcessors {
  /* ----------------------------- CONSTANTS ------------------------------ */

  /**
   * A block processor which will either return the original block's data or the data of the
   * previous processor, depending on if the previous result was null or not
   */
  public static final BlockProcessor NON_NULL_PROCESSOR = new NonNullProcessor();

  /**
   * A processor which rotates every given block, according to a given
   * {@link StructurePlaceConfig}'s rotation value
   */
  public static final BlockProcessor ROTATION_PROCESSOR = new RotationProcessor();

  /**
   * Block processor which will ignore all air blocks and not place them
   */
  public static final BlockProcessor IGNORE_AIR = new IgnoreAirProcessor();

  /* ----------------------------- FACTORIES ------------------------------ */

  public static BlockRotProcessor rot(float integrity, Random random) {
    return rot(IntegrityProvider.fixed(integrity), random);
  }

  public static BlockRotProcessor rot(IntegrityProvider provider, Random random) {
    return new BlockRotProcessor(provider, random);
  }

  /* ---------------------------- SUB CLASSES ----------------------------- */

  private static class NonNullProcessor implements BlockProcessor {

    @Override
    public @Nullable BlockInfo process(@NotNull BlockInfo original,
        @Nullable BlockInfo previous,
        @NotNull StructurePlaceConfig context,
        Mutable<Vector3i> position
    ) {
      return previous == null ? original : previous;
    }
  }

  private static class IgnoreAirProcessor implements BlockProcessor {

    @Override
    public @Nullable BlockInfo process(@NotNull BlockInfo original,
        @Nullable BlockInfo previous,
        @NotNull StructurePlaceConfig config,
        Mutable<Vector3i> position
    ) {
      if (previous == null) {
        return null;
      }

      if (previous.getData().getMaterial().isAir()) {
        return null;
      }

      return previous;
    }
  }

  private static class RotationProcessor implements BlockProcessor {

    @Override
    public @Nullable BlockInfo process(@NotNull BlockInfo original,
        @Nullable BlockInfo previous,
        @NotNull StructurePlaceConfig context,
        Mutable<Vector3i> position
    ) {
      if (previous == null
          || context.getTransform() == null
          || context.getTransform().isIdentity()
      ) {
        return previous;
      }
      Transform transform = context.getTransform();

      StructureRotation structRot = switch (transform.getRotation()) {
        case NONE -> StructureRotation.NONE;
        case CLOCKWISE_90 -> StructureRotation.CLOCKWISE_90;
        case CLOCKWISE_180 -> StructureRotation.CLOCKWISE_180;
        case COUNTERCLOCKWISE_90 -> StructureRotation.COUNTERCLOCKWISE_90;
      };

      BlockData data = previous.getData();
      data.rotate(structRot);

      return previous.withData(data);
    }
  }


}