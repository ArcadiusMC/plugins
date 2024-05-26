package net.arcadiusmc.entity.phys;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.arcadiusmc.entity.system.Transform;
import net.arcadiusmc.entity.util.Transformer;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class CollisionSystem extends IteratingSystem {

  public static final int SECTOR_SHIFT = 5;
  public static final int SECTOR_SIZE = 1 << SECTOR_SHIFT;
  public static final long X_BITS = 0xFFFFFF0000000000L;
  public static final long Y_BITS = 0x000000FFFF000000L;
  public static final long Z_BITS = 0x0000000000FFFFFFL;
  public static final long X_SHIFT = 40L;
  public static final long Y_SHIFT = 24L;
  public static final long Z_SHIFT = 0L;

  SectorOp op = new SectorOp();

  public CollisionSystem() {
    super(Family.all(CollisionComponent.class, Transform.class).get());
  }

  @Override
  protected void processEntity(Entity entity, float deltaTime) {
    Transform transform = entity.getComponent(Transform.class);
    CollisionComponent collision = entity.getComponent(CollisionComponent.class);

    if (collision.sectors == null) {
      collision.sectors = new LongOpenHashSet();
    }

    Vector3d dif = new Vector3d();
    transform.getPosition().sub(transform.getLastPosition(), dif);

    if (dif.x != 0 || dif.y != 0 || dif.z != 0) {
      Transformer transformer = Transformer.offset(dif);
      collision.shape.apply(transformer);
    }

    collision.shape.forEachSector(SECTOR_SHIFT, op);
  }

  static long packSectorCord(long x, long y, long z) {
    return (x << X_SHIFT) & X_BITS
         | (y << Y_SHIFT) & Y_BITS
         | (z << Z_SHIFT) & Z_BITS;
  }

  static void unpackSectorCord(long packed, Vector3i out) {
    long x = (packed & X_BITS) >> X_SHIFT;
    long y = (packed & Y_BITS) >> Y_SHIFT;
    long z = (packed & Z_BITS) >> Z_SHIFT;
    out.x = (int) x;
    out.y = (int) y;
    out.z = (int) z;
  }

  class SectorOp implements SectorConsumer {

    LongSet previousSet;
    LongSet currentSet;

    @Override
    public void accept(int x, int y, int z) {

    }
  }
}
