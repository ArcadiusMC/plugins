package net.arcadiusmc.utils.collision;

import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.World;

public interface CollisionLookup<T> {

  void getColliding(World world, Bounds3i bounds3i, CollisionSet<T> out);
}
