package net.arcadiusmc.entity.phys;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.entity.util.Transformer;
import net.arcadiusmc.utils.io.JomlCodecs;
import org.joml.Vector3d;

@Getter
public class AxisAlignedBounds implements Shape {

  static final Codec<AxisAlignedBounds> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            JomlCodecs.VEC3D.optionalFieldOf("min")
                .forGetter(aabb -> Optional.of(aabb.min)),

            JomlCodecs.VEC3D.optionalFieldOf("max")
                .forGetter(aabb -> Optional.of(aabb.max))
        )
        .apply(instance, (p1Opt, p2Opt) -> {
          AxisAlignedBounds bounds = new AxisAlignedBounds();

          Vector3d p1 = p1Opt.orElseGet(bounds::getMin);
          Vector3d p2 = p2Opt.orElseGet(bounds::getMax);

          bounds.resize(p1, p2);
          return bounds;
        });
  });

  private final Vector3d min = new Vector3d();
  private final Vector3d max = new Vector3d();

  public AxisAlignedBounds() {

  }

  public AxisAlignedBounds(Vector3d min, Vector3d max) {
    resize(min, max);
  }

  public AxisAlignedBounds resize(Vector3d p1, Vector3d p2) {
    p1.min(p2, this.min);
    p1.max(p2, this.max);
    return this;
  }

  @Override
  public AxisAlignedBounds getBoundingBox(AxisAlignedBounds out) {
    out.min.set(min);
    out.max.set(max);
    return out;
  }

  @Override
  public void apply(Transformer transformer) {
    Vector3d p1 = new Vector3d();
    Vector3d p2 = new Vector3d();

    transformer.transform(min.x, min.y, min.z, p1);
    transformer.transform(max.x, max.y, max.z, p2);

    resize(p1, p2);
  }

  @Override
  public ShapeType getType() {
    return ShapeType.AXIS_ALIGNED;
  }

  @Override
  public boolean isEmpty() {
    return min.x - max.x == 0
        && min.y - max.y == 0
        && min.z - max.z == 0;
  }

  public AxisAlignedBounds zero() {
    this.min.set(0);
    this.max.set(0);
    return this;
  }

  public AxisAlignedBounds combine(AxisAlignedBounds other) {
    other.min.min(this.min, this.min);
    other.max.max(this.max, this.max);
    return this;
  }

  @Override
  public void forEachSector(int shift, SectorConsumer consumer) {
    if (isEmpty()) {
      return;
    }

    int sectorMinX = (int) min.x >> shift;
    int sectorMinY = (int) min.y >> shift;
    int sectorMinZ = (int) min.z >> shift;

    int sectorMaxX = (int) max.x >> shift;
    int sectorMaxY = (int) max.y >> shift;
    int sectorMaxZ = (int) max.z >> shift;

    for (int x = sectorMinX; x <= sectorMaxX; x++) {
      for (int y = sectorMinY; y <= sectorMaxY; y++) {
        for (int z = sectorMinZ; z <= sectorMaxZ; z++) {
          consumer.accept(x, y, z);
        }
      }
    }

  }
}
