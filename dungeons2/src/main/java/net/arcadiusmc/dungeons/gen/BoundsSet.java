package net.arcadiusmc.dungeons.gen;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.utils.math.Bounds3i;
import org.spongepowered.math.vector.Vector3i;

public class BoundsSet {

  @Getter
  private final Set<Bounds3i> boundingBoxes = new ObjectOpenHashSet<>();
  private Bounds3i combined = Bounds3i.EMPTY;

  public void set(DungeonPiece rootPiece) {
    combined = rootPiece.getBoundingBox();
    boundingBoxes.clear();

    rootPiece.forEachDescendant(piece -> {
      Bounds3i bb = piece.getBoundingBox();
      combined = combined.combine(bb);
      boundingBoxes.add(bb);
    });
  }

  public Bounds3i combine() {
    return combined;
  }

  public boolean anyContains(Vector3i pos) {
    return anyContains(pos.x(), pos.y(), pos.z());
  }

  public boolean anyContains(int x, int y, int z) {
    if (!combined.contains(x, y, z)) {
      return false;
    }

    for (Bounds3i boundingBox : boundingBoxes) {
      if (!boundingBox.contains(x, y, z)) {
        continue;
      }

      return true;
    }

    return false;
  }

  public boolean anyContains(int x, int z) {
    if (!contains(combined, x, z)) {
      return false;
    }

    for (Bounds3i boundingBox : boundingBoxes) {
      if (!contains(boundingBox, x, z)) {
        continue;
      }

      return true;
    }

    return false;
  }

  private boolean contains(Bounds3i bb, int x, int z) {
    return x >= bb.minX() && x <= bb.maxX()
        && z >= bb.minZ() && z <= bb.maxZ();
  }

  public void forEachBlock(XyzFunction function) throws XyzIterationException {
    for (Bounds3i boundingBox : boundingBoxes) {
      int minX = boundingBox.minX();
      int minY = boundingBox.minY();
      int minZ = boundingBox.minZ();

      int maxX = boundingBox.maxX();
      int maxY = boundingBox.maxY();
      int maxZ = boundingBox.maxZ();

      for (int x = minX; x < maxX; x++) {
        for (int y = minY; y < maxY; y++) {
          for (int z = minZ; z < maxZ; z++) {
            try {
              function.accept(x, y, z);
            } catch (Throwable t) {
              throw new XyzIterationException(t, x, y ,z);
            }
          }
        }
      }
    }
  }
}
