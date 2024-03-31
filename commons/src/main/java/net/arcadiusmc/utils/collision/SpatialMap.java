package net.arcadiusmc.utils.collision;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import java.util.Set;
import net.arcadiusmc.utils.math.AbstractBounds3i;
import net.arcadiusmc.utils.math.Bounds3i;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public interface SpatialMap<T> extends CollisionLookup<T> {

  @NotNull Set<T> getOverlapping(@NotNull AbstractBounds3i<?> bounds3i);

  @NotNull Set<T> get(@NotNull Vector3i pos);

  boolean add(T value, Bounds3i bounds);

  @NotNull ObjectDoublePair<T> findNearest(@NotNull Vector3d point);

  boolean remove(T value);

  void clear();

  int size();

  boolean isEmpty();

  Set<T> values();
}
