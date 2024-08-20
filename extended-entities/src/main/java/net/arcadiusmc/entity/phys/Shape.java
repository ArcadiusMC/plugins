package net.arcadiusmc.entity.phys;

import net.arcadiusmc.entity.util.Transformer;

public interface Shape {

  AxisAlignedBounds getBoundingBox(AxisAlignedBounds out);

  default AxisAlignedBounds getBoundingBox() {
    return getBoundingBox(new AxisAlignedBounds());
  }

  void apply(Transformer transformer);

  ShapeType getType();

  void forEachSector(int sectorShift, SectorConsumer consumer);

  boolean isEmpty();
}
