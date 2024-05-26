package net.arcadiusmc.entity.phys;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.arcadiusmc.entity.util.Transformer;

@Getter
public class ConcatShape implements Shape {

  public static final Codec<ConcatShape> CODEC = ShapeType.SHAPE_CODEC.listOf()
      .xmap(
          shapes -> {
            ConcatShape shape = new ConcatShape();
            shape.elements.addAll(shapes);
            return shape;
          },
          ConcatShape::getElements
      );

  private final List<Shape> elements = new ArrayList<>();

  @Override
  public AxisAlignedBounds getBoundingBox(AxisAlignedBounds out) {
    if (elements.isEmpty()) {
      return out.zero();
    }

    AxisAlignedBounds elementOut = new AxisAlignedBounds();

    for (Shape element : elements) {
      element.getBoundingBox(elementOut);
      out.combine(elementOut);
    }

    return out;
  }

  @Override
  public void apply(Transformer transformer) {
    if (elements.isEmpty()) {
      return;
    }

    for (Shape element : elements) {
      element.apply(transformer);
    }
  }

  @Override
  public ShapeType getType() {
    return ShapeType.CONCAT;
  }

  @Override
  public void forEachSector(int shift, SectorConsumer consumer) {
    for (Shape element : elements) {
      element.forEachSector(shift, consumer);
    }
  }

  @Override
  public boolean isEmpty() {
    return elements.isEmpty();
  }
}
