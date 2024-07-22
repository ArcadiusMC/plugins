package net.arcadiusmc.dungeons.placement;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockPalette;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.Structures;
import net.arcadiusmc.structure.pool.StructureAndPalette;
import net.arcadiusmc.structure.pool.StructurePool;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import org.slf4j.Logger;

@Getter
public class PoolProcessor implements PostPlacementProcessor {

  private static final Logger LOGGER = Loggers.getLogger();

  static final String KEY_POOL = "pool";

  private final Registry<StructurePool> pools;
  private final Registry<BlockStructure> structures;

  public PoolProcessor(Structures structures) {
    this.pools = structures.getPoolRegistry();
    this.structures = structures.getRegistry();
  }

  @Override
  public void processAll(LevelPlacement placement, List<FunctionInfo> markerList, Random random) {
    if (markerList.isEmpty()) {
      return;
    }

    for (FunctionInfo info : markerList) {
      process(placement, info, random);
    }
  }

  private void process(LevelPlacement placement, FunctionInfo info, Random random) {
    String poolKey = info.getTag().getString(KEY_POOL);
    Optional<Holder<StructurePool>> opt = pools.getHolder(poolKey);

    if (opt.isEmpty()) {
      LOGGER.error("Unknown structure pool {}", poolKey);
      return;
    }

    Holder<StructurePool> poolHolder = opt.get();
    StructurePool pool = poolHolder.getValue();

    Optional<StructureAndPalette> structOpt = pool.getRandom(structures, random);

    if (structOpt.isEmpty()) {
      LOGGER.error("Failed to replace a pool, pool={}", poolHolder.getKey());
      return;
    }

    StructureAndPalette sp = structOpt.get();
    Holder<BlockStructure> holder = sp.structure();
    BlockPalette palette = holder.getValue().getPalette(sp.paletteName());

    if (palette == null) {
      LOGGER.error("Palette {} doesn't exist in structure {}", sp.paletteName(), holder.getKey());
      return;
    }

    Rotation rotation = switch (info.getFacing()) {
      case EAST -> Rotation.CLOCKWISE_90;
      case WEST -> Rotation.COUNTERCLOCKWISE_90;
      case SOUTH -> Rotation.CLOCKWISE_180;
      default -> Rotation.NONE;
    };

    StructurePlaceConfig config = StructurePlaceConfig.builder()
        .paletteName(sp.paletteName())
        .buffer(placement.getBuffer())
        .entitySpawner(placement.getEntityPlacement())
        .pos(info.getOffset())
        .transform(Transform.rotation(rotation))
        .addNonNullProcessor()
        .addRotationProcessor()
        .build();

    holder.getValue().place(config);
  }
}