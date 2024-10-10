package net.arcadiusmc.structure.buffer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Transform;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.Nullable;

@Getter
public class ImmediateBlockBuffer implements BlockBuffer {
  private final World world;

  ImmediateBlockBuffer(World world) {
    this.world = Objects.requireNonNull(world);
  }

  @Override
  public CompletableFuture<Void> place(World world, Transform transform, boolean updatePhysics) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public BlockState getBlock(int x, int y, int z) {
    Block block = world.getBlockAt(x, y, z);
    return block.getState();
  }

  @Override
  public void setBlock(int x, int y, int z, BlockState state) {
    if (state == null) {
      clearBlock(x, y, z);
      return;
    }

    Location l = new Location(world, x, y, z);
    state = state.copy(l);

    state.update(true, false);
  }

  @Override
  public void clearBlock(int x, int y, int z) {
    Block b = world.getBlockAt(x, y, z);
    b.setType(Material.AIR, false);
  }

  @Override
  public @Nullable Bounds3i getBounds() {
    return null;
  }
}