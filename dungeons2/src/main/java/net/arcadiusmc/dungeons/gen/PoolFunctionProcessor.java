package net.arcadiusmc.dungeons.gen;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.Random;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.FunctionProcessor;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructurePlaceConfig.Builder;
import net.arcadiusmc.structure.Structures;
import net.arcadiusmc.structure.StructuresPlugin;
import net.arcadiusmc.structure.buffer.BlockBuffer;
import net.arcadiusmc.structure.pool.StructureAndPalette;
import net.arcadiusmc.structure.pool.StructurePool;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

public class PoolFunctionProcessor implements FunctionProcessor {

  static final int MAX_DEPTH = 16;

  static final String TAG_ROTATION = "rotation";
  static final String TAG_OFFSET = "offset";
  static final String TAG_POOL_NAME = "pool_name";
  static final String TAG_DEEP = "deep";
  static final String TAG_ALIGN_POINT = "use_alignment_point";
  static final String TAG_CHECK_COLLISION = "check_collision";
  static final String TAG_CHANCE = "gen_chance";

  private static final Logger LOGGER = Loggers.getLogger();

  private final BlockBuffer buffer;
  private final DungeonGenerator generator;
  private final Random random;

  private int depth = 0;

  public PoolFunctionProcessor(BlockBuffer buffer, Random random, DungeonGenerator generator) {
    this.buffer = buffer;
    this.random = random;
    this.generator = generator;
  }

  @Override
  public void process(@NotNull FunctionInfo info, @NotNull StructurePlaceConfig config) {
    if (depth >= MAX_DEPTH) {
      return;
    }

    CompoundTag data = info.getTag();
    if (data == null || data.isEmpty()) {
      return;
    }

    String poolKey = data.getString(TAG_POOL_NAME);
    if (Strings.isNullOrEmpty(poolKey)) {
      return;
    }

    Structures structures = StructuresPlugin.getManager();
    Registry<StructurePool> registry = structures.getPoolRegistry();
    Optional<StructurePool> opt = registry.get(poolKey);

    if (opt.isEmpty()) {
      LOGGER.warn("Unknown structure pool '{}'", poolKey);
      return;
    }

    StructurePool pool = opt.get();
    Optional<StructureAndPalette> structOpt = pool.getRandom(structures.getRegistry(), random);

    if (structOpt.isEmpty()) {
      LOGGER.warn("Pool '{}' returned a non-existing structure", poolKey);
      return;
    }

    float rate = data.getFloat(TAG_CHANCE, 1f);
    if (rate < 1f) {
      float rand = generator.getRandom().nextFloat();

      if (rand >= rate) {
        return;
      }
    }

    StructureAndPalette result = structOpt.get();
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

    if (data.getBoolean(TAG_ALIGN_POINT, true)) {
      Direction direction = info.getFacing();
      if (direction.isRotatable()) {
        direction = direction.rotate(rotation);
      }

      Transform t = applyAlign(result.structure().getValue(), rotation, direction);
      if (!t.isIdentity()) {
        transform = transform.combine(t);
      }
    }

    Vector3i position = originalRotate.rotate(info.getOffset());

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

    depth++;
    try {
      result.structure().getValue().place(cfg);
    } finally {
      depth--;
    }

    generator.collectFunctions(result.structure(), cfg);
  }

  private FunctionInfo getAlignmentPoint(BlockStructure structure) {
    return structure.getFunctions().stream()
        .filter(info -> info.getFunctionKey().equals(LevelFunctions.ALIGNMENT_POINT))
        .findFirst()
        .orElse(null);
  }

  private Transform applyAlign(BlockStructure target, Rotation rotation, Direction currentDir) {
    FunctionInfo targetPoint = getAlignmentPoint(target);

    if (targetPoint == null) {
      return Transform.IDENTITY;
    }

    Vector3i off = rotation.rotate(targetPoint.getOffset());
    Direction facing = targetPoint.getFacing();

    if (facing.isRotatable()) {
      facing = facing.rotate(rotation);
    }

    Rotation transRotate;

    if (currentDir == facing || !currentDir.isRotatable() || !facing.isRotatable()) {
      transRotate = Rotation.NONE;
    } else {
      transRotate = facing.deriveRotationFrom(currentDir);
      off = transRotate.rotate(off);
    }

    return Transform.offset(off.mul(-1)).addRotation(transRotate);
  }
}
