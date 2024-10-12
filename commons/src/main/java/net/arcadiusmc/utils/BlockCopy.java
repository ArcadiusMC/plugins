package net.arcadiusmc.utils;

import net.arcadiusmc.utils.math.WorldBounds3i;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.spongepowered.math.vector.Vector3i;

public final class BlockCopy {
  private BlockCopy() {}

  public static void copyBlocks(
      Vector3i originA,
      Vector3i originB,
      Vector3i destination,
      World world
  ) {
    WorldBounds3i bounds = WorldBounds3i.of(world, originA, originB);
    Vector3i min = bounds.min();
    Location l = new Location(world, 0, 0, 0);

    for (var block : bounds) {
      int offX = block.getX() - min.x();
      int offY = block.getY() - min.y();
      int offZ = block.getZ() - min.z();

      Vector3i destPos = destination.add(offX, offY, offZ);

      l.setX(destPos.x());
      l.setY(destPos.y());
      l.setZ(destPos.z());

      BlockState state = block.getState();
      BlockState copy = state.copy(l);

      copy.update(true, false);
    }
  }
}
