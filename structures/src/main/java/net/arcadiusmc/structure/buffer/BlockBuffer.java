package net.arcadiusmc.structure.buffer;

import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Transform;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.vector.Vector3i;

public interface BlockBuffer {

  default CompletableFuture<Void> place(World world) {
    return place(world, Transform.IDENTITY);
  }

  default CompletableFuture<Void> place(World world, Transform transform) {
    return place(world, transform, false);
  }

  CompletableFuture<Void> place(World world, Transform transform, boolean updatePhysics);

  default BlockState getBlock(Vector3i pos) {
    return getBlock(pos.x(), pos.y(), pos.z());
  }

  @Nullable BlockState getBlock(int x, int y, int z);

  default void setBlock(int x, int y, int z, BlockData data) {
    setBlock(x, y, z, data.createBlockState());
  }

  void clearBlock(int x, int y, int z);

  @Nullable Bounds3i getBounds();

  default boolean isBoundaryLimited() {
    return getBounds() == null;
  }

  void setBlock(int x, int y, int z, BlockState state);
}