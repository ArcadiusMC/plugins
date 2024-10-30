package net.arcadiusmc.dungeons.gen;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Unit;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.List;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockPalette;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructurePlaceConfig.Builder;
import net.arcadiusmc.structure.Structures;
import net.arcadiusmc.structure.StructuresPlugin;
import net.arcadiusmc.structure.pool.StructureAndPalette;
import net.arcadiusmc.structure.pool.StructurePool;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.nbt.CompoundTag;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

public class PoolDecorator extends Decorator<Unit> {

  static final int MAX_DEPTH = 16;

  static final String TAG_ROTATION = "rotation";
  static final String TAG_OFFSET = "offset";
  static final String TAG_POOL_NAME = "pool_name";
  static final String TAG_DEEP = "deep";
  static final String TAG_ALIGN_POINT = "use_alignment_point";
  static final String TAG_CHECK_COLLISION = "check_collision";
  static final String TAG_CHANCE = "gen_chance";

  private static final Logger LOGGER = Loggers.getLogger();

  public PoolDecorator() {
    super(Unit.INSTANCE);
  }

  @Override
  public void execute() {
    List<GeneratorFunction> poolFunctions = getFunctions(LevelFunctions.POOL);

    for (GeneratorFunction poolFunction : poolFunctions) {
      placePool(poolFunction);
    }
  }

  private void placePool(GeneratorFunction function) {
    if (function.getDepth() > MAX_DEPTH) {
      return;
    }

    CompoundTag data = function.getData();
    float chance = data.getFloat(TAG_CHANCE, 1f);

    if (!randomBool(chance)) {
      return;
    }

    if (function.getDepth() > 0 && !data.getBoolean(TAG_DEEP, false)) {
      return;
    }

    StructureAndPalette result = resolveStructure(data);
    Transform transform = Transform.IDENTITY;

    if (result == null) {
      return;
    }

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
      Direction direction = function.getFacing();
      if (direction.isRotatable()) {
        direction = direction.rotate(rotation);
      }

      Transform t = applyAlign(result.structure().getValue(), rotation, direction);
      if (!t.isIdentity()) {
        transform = transform.combine(t);
      }
    }

    Vector3i position = function.getPosition();

    if (!checkCollision(result, transform, position, data)) {
      return;
    }

    Builder builder = StructurePlaceConfig.builder()
        .pos(position)
        .buffer(generator.getBuffer())
        .paletteName(result.paletteName())
        .addNonNullProcessor()
        .addRotationProcessor()
        //.addProcessor(BlockProcessors.IGNORE_AIR)
        .transform(transform);

    StructurePlaceConfig cfg = builder.build();

    result.structure().getValue().place(cfg);

    generator.collectFunctions(
        function.getContainingPiece(),
        function.getDepth() + 1,
        result.structure(),
        cfg
    );
  }

  private boolean checkCollision(
      StructureAndPalette struct,
      Transform t,
      Vector3i pos,
      CompoundTag data
  ) {
    if (!data.getBoolean(TAG_CHECK_COLLISION)) {
      return true;
    }

    t = t.addOffset(pos);

    BlockPalette palette = struct.structure().getValue().getPalette(struct.paletteName());
    if (palette == null) {
      return false;
    }

    for (LongList value : palette.getBlock2Positions().values()) {
      for (int i = 0; i < value.size(); i++) {
        long packed = value.getLong(i);
        Vector3i blockPos = t.apply(Vectors.fromLong(packed));

        if (isAir(blockPos.x(), blockPos.y(), blockPos.z())) {
          continue;
        }

        return false;
      }
    }

    return true;
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

  private StructureAndPalette resolveStructure(CompoundTag data) {
    String poolKey = data.getString(TAG_POOL_NAME);
    if (Strings.isNullOrEmpty(poolKey)) {
      return null;
    }

    Structures structures = StructuresPlugin.getManager();
    Registry<StructurePool> registry = structures.getPoolRegistry();
    Optional<StructurePool> opt = registry.get(poolKey);

    if (opt.isEmpty()) {
      LOGGER.warn("Unknown structure pool '{}'", poolKey);
      return null;
    }

    StructurePool pool = opt.get();
    Optional<StructureAndPalette> structOpt = pool.getRandom(structures.getRegistry(), random);

    if (structOpt.isEmpty()) {
      LOGGER.warn("Pool '{}' returned a non-existing structure", poolKey);
      return null;
    }

    return structOpt.get();
  }
}
