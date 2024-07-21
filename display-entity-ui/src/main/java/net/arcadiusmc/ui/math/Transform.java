package net.arcadiusmc.ui.math;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Transform {

  public final Vector3f translation = new Vector3f();
  public final Vector3f scale = new Vector3f();
  public final Quaternionf rotation = new Quaternionf();
  public final Matrix4f matrix = new Matrix4f();

  public void calculateMatrix() {
    matrix.identity();
    matrix.translate(translation);
    matrix.rotate(rotation);
    matrix.scale(scale);
  }

  public void apply(Vector3f position) {
    position.mulPosition(matrix);
  }
}
