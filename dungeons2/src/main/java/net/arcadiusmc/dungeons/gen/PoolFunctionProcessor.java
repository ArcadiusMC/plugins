package net.arcadiusmc.dungeons.gen;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.Random;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.FunctionProcessor;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructurePlaceConfig.Builder;
import net.arcadiusmc.structure.Structures;
import net.arcadiusmc.structure.buffer.BlockBuffer;
import net.arcadiusmc.structure.pool.StructureAndPalette;
import net.arcadiusmc.structure.pool.StructurePool;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

public class PoolFunctionProcessor implements FunctionProcessor {

  static final String TAG_ROTATION = "rotation";
  static final String TAG_OFFSET = "offset";
  static final String TAG_POOL_NAME = "pool_name";
  static final String TAG_DEEP = "deep";

  private static final Logger LOGGER = Loggers.getLogger();

  private final BlockBuffer buffer;
  private final DungeonGenerator generator;
  private final Random random;

  public PoolFunctionProcessor(BlockBuffer buffer, Random random, DungeonGenerator generator) {
    this.buffer = buffer;
    this.random = random;
    this.generator = generator;
  }

  @Override
  public void process(@NotNull FunctionInfo info, @NotNull StructurePlaceConfig config) {
    CompoundTag data = info.getTag();
    if (data == null || data.isEmpty()) {
      return;
    }

    String poolKey = data.getString(TAG_POOL_NAME);
    if (Strings.isNullOrEmpty(poolKey)) {
      return;
    }

    Structures structures = Structures.get();
    Registry<StructurePool> registry = structures.getPoolRegistry();
    Optional<StructurePool> opt = registry.get(poolKey);

    if (opt.isEmpty()) {
      LOGGER.warn("Unknown structure pool '{}'", poolKey);
      return;
    }

    Transform transform = config.getTransform();
    Rotation originalRotate = transform.getRotation();

    if (data.contains(TAG_ROTATION)) {
      String rotation = data.getString(TAG_ROTATION, "none");
      Rotation rot = switch (rotation.toLowerCase()) {
        case "d90", "clockwise_90" -> Rotation.CLOCKWISE_90;
        case "d180", "clockwise_180" -> Rotation.CLOCKWISE_180;
        case "d270", "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
        default -> Rotation.NONE;
      };

      transform = transform.addRotation(rot);
    }

    Rotation rotation = transform.getRotation();

    if (data.contains(TAG_OFFSET)) {
      Vector3i transformOffset = Vectors.read3i(data.get(TAG_OFFSET));
      transform = transform.addOffset(rotation.rotate(transformOffset));
    }

    StructurePool pool = opt.get();
    Vector3i position = originalRotate.rotate(info.getOffset());

    Optional<StructureAndPalette> structOpt = pool.getRandom(structures.getRegistry(), random);
    if (structOpt.isEmpty()) {
      LOGGER.warn("Pool '{}' returned a non-existing structure", poolKey);
      return;
    }

    StructureAndPalette result = structOpt.get();

    Builder builder = StructurePlaceConfig.builder()
        .pos(position)
        .buffer(buffer)
        .paletteName(result.paletteName())
        .addNonNullProcessor()
        .addRotationProcessor()
        //.addProcessor(BlockProcessors.IGNORE_AIR)
        .transform(transform);

    if (data.getBoolean(TAG_DEEP, false)) {
      builder.addFunction(LevelFunctions.POOL, this);
    }

    StructurePlaceConfig cfg = builder.build();

    result.structure().getValue().place(cfg);
    generator.collectFunctions(result.structure(), cfg);
  }
}
