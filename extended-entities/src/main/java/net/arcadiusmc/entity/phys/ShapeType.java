package net.arcadiusmc.entity.phys;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.arcadiusmc.utils.io.ExtraCodecs;

@Getter
public enum ShapeType {
  AXIS_ALIGNED (AxisAlignedBounds.CODEC),
  ARRAY (ArrayShape.CODEC),
  CONCAT (ConcatShape.CODEC);

  public static final Codec<Shape> SHAPE_CODEC;

  static {
    Codec<ShapeType> typeCodec = ExtraCodecs.enumCodec(ShapeType.class);
    SHAPE_CODEC = typeCodec.dispatch(Shape::getType, ShapeType::getMapCodec);
  }

  final Codec<Shape> codec;
  final MapCodec<Shape> mapCodec;

  ShapeType(Codec<? extends Shape> codec) {
    this.codec = (Codec<Shape>) codec;
    this.mapCodec = this.codec.fieldOf("shape_data");
  }
}
