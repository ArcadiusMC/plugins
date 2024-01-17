package net.arcadiusmc;

import com.google.common.collect.Iterators;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.util.Iterator;
import net.arcadiusmc.utils.math.AreaSelection;
import net.arcadiusmc.utils.math.WorldBounds3i;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3i;

/**
 * An abstraction of common parts of the WorldEdit API, so individual plugins don't have to depend
 * on the WorldEdit plugin for 1 or 2 features
 */
public final class WorldEditHook {
  private WorldEditHook() {}

  public static AreaSelection getSelectedBlocks(Player player) {
    com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);

    try {
      Region selection = wePlayer.getSession().getSelection();
      return new WorldEditSelection(selection);
    } catch (IncompleteRegionException exc) {
      return null;
    }
  }

  public static WorldBounds3i getPlayerSelection(Player player) {
    com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);

    try {
      Region selection = wePlayer.getSession().getSelection();
      CuboidRegion cube = selection.getBoundingBox();

      return new WorldBounds3i(
          BukkitAdapter.adapt(selection.getWorld()),
          cube.getMinimumX(),
          cube.getMinimumY(),
          cube.getMinimumZ(),
          cube.getMaximumX(),
          cube.getMaximumY(),
          cube.getMaximumZ()
      );

    } catch (IncompleteRegionException exc) {
      return null;
    }
  }

  record WorldEditSelection(Region region) implements AreaSelection {

    @Override
    public World getWorld() {
      return BukkitAdapter.adapt(region.getWorld());
    }

    @Override
    public Iterator<Entity> entities() {
      var world = getWorld();
      var min = min();
      var max = max();

      var bounds = WorldBounds3i.of(world, min, max);

      return Iterators.filter(bounds.entities(), input -> {
        var pos = input.getLocation();
        return region.contains(pos.blockX(), pos.blockY(), pos.blockZ());
      });
    }

    @NotNull
    @Override
    public Iterator<Block> iterator() {
      return new Iterator<>() {
        final World world = getWorld();
        final Iterator<BlockVector3> backing = region.iterator();

        @Override
        public boolean hasNext() {
          return backing.hasNext();
        }

        @Override
        public void remove() {
          backing.remove();
        }

        @Override
        public Block next() {
          BlockVector3 bPos = backing.next();
          return world.getBlockAt(bPos.getX(), bPos.getY(), bPos.getZ());
        }
      };
    }

    @Override
    public Vector3i min() {
      var min = region.getMinimumPoint();
      return Vector3i.from(min.getX(), min.getY(), min.getZ());
    }

    @Override
    public Vector3i max() {
      var min = region.getMaximumPoint();
      return Vector3i.from(min.getX(), min.getY(), min.getZ());
    }

    @Override
    public Vector3i size() {
      return max().sub(min());
    }
  }
}