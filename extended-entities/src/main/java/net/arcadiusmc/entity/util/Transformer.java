package net.arcadiusmc.entity.util;

import org.joml.Matrix3d;
import org.joml.Vector3d;

public interface Transformer {

  static Transformer matrix(Matrix3d matrix3f) {
    return new MatrixTransformer(matrix3f);
  }

  static Transformer combine(Transformer... transformers) {
    return new CombinedTransformer(transformers);
  }

  static Transformer offset(Vector3d off) {
    return new OffsetTransformer(off);
  }

  void transform(double x, double y, double z, Vector3d out);

  default Transformer pivotAround(Vector3d pos) {
    return new PivotedTransformer(pos, this);
  }

  record CombinedTransformer(Transformer[] transformers) implements Transformer {

    @Override
    public void transform(double x, double y, double z, Vector3d out) {
      if (transformers.length < 1) {
        return;
      }

      for (Transformer transformer : transformers) {
        transformer.transform(x, y, z, out);
        x = out.x;
        y = out.y;
        z = out.z;
      }
    }
  }

  record OffsetTransformer(Vector3d offset) implements Transformer {

    @Override
    public void transform(double x, double y, double z, Vector3d out) {
      out.set(x + offset.x, y + offset.y, z + offset.z);
    }
  }

  record PivotedTransformer(Vector3d pivot, Transformer transformer) implements Transformer {

    @Override
    public void transform(double x, double y, double z, Vector3d out) {
      transformer.transform(x - pivot.x, y - pivot.y, z - pivot.z, out);
      out.add(pivot);
    }
  }

  record MatrixTransformer(Matrix3d matrix3f) implements Transformer {

    @Override
    public void transform(double x, double y, double z, Vector3d out) {
      matrix3f.transform(x, y, z, out);
    }
  }
}
