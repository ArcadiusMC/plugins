package net.arcadiusmc.entity.phys;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.entity.util.Transformer;
import org.joml.Vector3d;

@Getter
public class ArrayShape implements Shape {

  private static final Codec<double[]> FLOAT_ARR_CODEC = Codec.DOUBLE.listOf()
      .xmap(
          doubles -> {
            double[] arr = new double[doubles.size()];
            for (int i = 0; i < arr.length; i++) {
              arr[i] = doubles.get(i);
            }
            return arr;
          },
          arr -> {
            List<Double> doubles = new ArrayList<>();
            for (double v : arr) {
              doubles.add(v);
            }
            return doubles;
          }
      );

  public static final Codec<ArrayShape> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            FLOAT_ARR_CODEC.optionalFieldOf("x_positions", DoubleArrays.EMPTY_ARRAY)
                .forGetter(shape -> shape.xPositions),
            FLOAT_ARR_CODEC.optionalFieldOf("y_positions", DoubleArrays.EMPTY_ARRAY)
                .forGetter(shape -> shape.yPositions),
            FLOAT_ARR_CODEC.optionalFieldOf("z_positions", DoubleArrays.EMPTY_ARRAY)
                .forGetter(shape -> shape.zPositions)
        )
        .apply(instance, ArrayShape::new);
  });

  private double[] xPositions = DoubleArrays.EMPTY_ARRAY;
  private double[] yPositions = DoubleArrays.EMPTY_ARRAY;
  private double[] zPositions = DoubleArrays.EMPTY_ARRAY;

  public ArrayShape(double[] x, double[] y, double[] z) {
    set(x, y, z);
  }

  public ArrayShape() {

  }

  public ArrayShape set(double[] x, double[] y, double[] z) {
    Objects.requireNonNull(x, "Null x positions");
    Objects.requireNonNull(y, "Null y positions");
    Objects.requireNonNull(z, "Null z positions");

    Preconditions.checkArgument(x.length == y.length && y.length == z.length,
        "Position arrays must be of same size"
    );

    Preconditions.checkArgument(x.length >= 3,
        "There must be at least 3 positions"
    );

    this.xPositions = x;
    this.yPositions = y;
    this.zPositions = z;

    return this;
  }

  public int pointCount() {
    return xPositions.length;
  }

  @Override
  public AxisAlignedBounds getBoundingBox(AxisAlignedBounds out) {
    int points = xPositions.length;

    if (points < 1) {
      return out.zero();
    }

    Vector3d min = new Vector3d(Float.MAX_VALUE);
    Vector3d max = new Vector3d(Float.MIN_VALUE);

    double x;
    double y;
    double z;

    for (int i = 0; i < points; i++) {
      x = xPositions[i];
      y = yPositions[i];
      z = zPositions[i];

      min.x = Math.min(min.x, x);
      min.y = Math.min(min.y, y);
      min.z = Math.min(min.z, z);

      max.x = Math.max(max.x, x);
      max.y = Math.max(max.y, y);
      max.z = Math.max(max.z, z);
    }

    return out.resize(min, max);
  }

  @Override
  public void apply(Transformer transformer) {
    int points = xPositions.length;

    if (points < 1) {
      return;
    }

    Vector3d out = new Vector3d();

    for (int i = 0; i < points; i++) {
      transformer.transform(
          xPositions[i],
          yPositions[i],
          zPositions[i],
          out
      );

      xPositions[i] = out.x;
      yPositions[i] = out.y;
      zPositions[i] = out.z;
    }
  }

  @Override
  public ShapeType getType() {
    return ShapeType.ARRAY;
  }

  @Override
  public void forEachSector(int shift, SectorConsumer consumer) {
    getBoundingBox().forEachSector(shift, consumer);
  }

  @Override
  public boolean isEmpty() {
    return xPositions.length < 1;
  }
}
