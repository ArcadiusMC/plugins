package net.arcadiusmc.items.guns;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;
import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class BlockDamage {

  public static boolean ENABLED = true;

  public static Set<Material> DAMAGEABLE = Set.of(
      Material.CRACKED_STONE_BRICKS,
      // Material.COBBLESTONE,
      // Material.COBBLESTONE_STAIRS,
      // Material.COBBLESTONE_SLAB,
      // Material.COBBLESTONE_WALL,
      // Material.ANDESITE,
      // Material.ANDESITE_STAIRS,
      // Material.ANDESITE_SLAB,
      // Material.ANDESITE_WALL,
      Material.DIORITE,
      Material.DIORITE_STAIRS,
      Material.DIORITE_SLAB,
      Material.DIORITE_WALL,
      Material.GRANITE,
      Material.GRANITE_STAIRS,
      Material.GRANITE_SLAB,
      Material.GRANITE_WALL
  );

  public static final BlockDamage BLOCK_DAMAGE = new BlockDamage();

  private final Map<String, Long2FloatMap> damageByWorlds = new HashMap<>();

  public void clear(World world, Bounds3i bounds3i) {
    if (!ENABLED) {
      return;
    }

    if (bounds3i == null) {
      damageByWorlds.remove(world.getName());
      return;
    }

    Long2FloatMap map = damageByWorlds.get(world.getName());

    if (map == null) {
      return;
    }

    forEachBlock(bounds3i, map::remove);
  }

  public void damage(Block block, float damage) {
    damage(block.getWorld(), block.getX(), block.getY(), block.getZ(), damage);
  }

  public void damage(World world, int x, int y, int z, float damage) {
    if (!ENABLED) {
      return;
    }

    Block block = world.getBlockAt(x, y, z);

    if (!DAMAGEABLE.contains(block.getType())) {
      return;
    }

    Long2FloatMap map = damageByWorlds.computeIfAbsent(
        world.getName(),
        string -> new Long2FloatOpenHashMap()
    );

    long key = Block.getBlockKey(x, y, z);
    float existingDamage = map.get(key);
    float newDamage = existingDamage + damage;

    float blastResistance = block.getType().getBlastResistance();

    if (newDamage >= blastResistance) {
      block.breakNaturally(new ItemStack(Material.AIR, 0), false, false);
      map.remove(key);
    } else {
      map.put(key, newDamage);
    }
  }

  private void forEachBlock(Bounds3i bounds, LongConsumer consumer) {
    int minX = bounds.minX();
    int minY = bounds.minY();
    int minZ = bounds.minZ();

    int maxX = bounds.maxX();
    int maxY = bounds.maxY();
    int maxZ = bounds.maxZ();

    for (int x = minX; x < maxX; x++) {
      for (int y = minY; y < maxY; y++) {
        for (int z = minZ; z < maxZ; z++) {
          long pos = Block.getBlockKey(x, y, z);
          consumer.accept(pos);
        }
      }
    }
  }
}
