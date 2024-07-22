package net.arcadiusmc.dungeons.placement;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.BiomeSource;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelBiome;
import net.arcadiusmc.dungeons.PieceVisitor;
import net.arcadiusmc.dungeons.gate.GatePiece;
import net.arcadiusmc.dungeons.room.RoomPiece;
import net.arcadiusmc.structure.BlockProcessors;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.StructurePlaceConfig.Builder;
import net.arcadiusmc.utils.math.Transform;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3d;

@RequiredArgsConstructor
public class RoomPlacingVisitor implements PieceVisitor {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final LevelPlacement placement;
  private final Lock lock = new ReentrantLock();

  @Getter
  private AtomicInteger placementCounter = new AtomicInteger(0);
  private AtomicInteger roomCount = new AtomicInteger(0);
  private boolean finishCalled = false;

  @Override
  public Result onGate(GatePiece gate) {
    addPlaceTask(gate);
    return Result.CONTINUE;
  }

  @Override
  public Result onRoom(RoomPiece room) {
    addPlaceTask(room);
    return Result.CONTINUE;
  }

  private void addPlaceTask(DungeonPiece piece) {
    placement.getExecutorService().execute(new PlacementTask(piece, placement, this));
  }

  public synchronized boolean isFinished() {
    return placementCounter.get() >= roomCount.get();
  }

  void onPlaced() {
    lock.lock();
    placementCounter.incrementAndGet();

    if (isFinished() && !finishCalled) {
      finishCalled = true;
      placement.onFinished();
    }

    lock.unlock();
  }

  record PlacementTask(
      DungeonPiece piece,
      LevelPlacement placement,
      RoomPlacingVisitor visitor
  ) implements Runnable {

    @Override
    public void run() {
      BlockStructure struct = piece.getStructure();

      if (struct == null) {
        LOGGER.error(
            "Cannot place piece {}, at {}, no structure with name {}",
            piece,
            piece.getPivotPosition(),
            piece.getType().getStructureName()
        );

        return;
      }

      visitor.roomCount.getAndIncrement();
      Vector3d center = piece.getBounds().center();
      Random random = placement.getRandom();

      BiomeSource source = placement.getBiomeSource();
      LevelBiome biome  = source.findBiome(center);

      Builder builder = StructurePlaceConfig.builder()
          .pos(piece.getPivotPosition())
          .transform(Transform.rotation(piece.getRotation()))

          .buffer(placement.getBuffer())
          .entitySpawner(placement.getEntityPlacement())

          .paletteName(piece.getPaletteName(biome))

          .addNonNullProcessor()
          .addRotationProcessor()
          .addProcessor(BlockProcessors.IGNORE_AIR)
          .addProcessor(BlockProcessors.rot(placement, random));

      StructurePlaceConfig config = builder.build();
      struct.place(config);

      struct.getFunctions().forEach(func -> {
        if (!func.getFunctionKey().startsWith("post/")) {
          return;
        }

        FunctionInfo info = func.withOffset(config.getTransform().apply(func.getOffset()));
        placement.addMarker(info);
      });

      visitor.onPlaced();
    }
  }
}