package net.arcadiusmc.ui.math;

import com.sk89q.worldedit.math.Vector3;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Screen {

  public static final float DEFAULT_WIDTH = 3;
  public static final float DEFAULT_HEIGHT = 2;

  private static final float EPSILON = 0.0000001f;

  private final Vector3f lowerLeft = new Vector3f();
  private final Vector3f lowerRight = new Vector3f();
  private final Vector3f upperLeft = new Vector3f();
  private final Vector3f upperRight = new Vector3f();

  private final Vector3f normal = new Vector3f();
  private final Vector3f center = new Vector3f();

  @Getter
  private float width;
  @Getter
  private float height;

  public Screen() {
    set(new Vector3f(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public void set(Vector3f center, float width, float height) {
    float halfWidth = width * 0.5f;

    Vector3f bottomLeft = new Vector3f(center);
    Vector3f topRight = new Vector3f(center);

    bottomLeft.sub(0, 0, halfWidth);
    topRight.add(0, height, halfWidth);

    this.upperRight.set(bottomLeft.x, topRight.y, bottomLeft.z);
    this.lowerLeft.set(topRight.x, bottomLeft.y, topRight.z);

    this.upperLeft.set(lowerLeft.x, upperRight.y, lowerLeft.z);
    this.lowerRight.set(upperRight.x, lowerLeft.y, upperRight.z);

    this.center.set(center);
    this.center.add(0, height * 0.5f, 0);

    this.normal.set(1, 0, 0);

    this.width = width;
    this.height = height;
  }

  public Location getLowerRightLocation(World world) {
    Vector2f rot = getRotation();
    return new Location(world, lowerRight.x, lowerRight.y, lowerRight.z, rot.x, rot.y);
  }

  public Vector3f getLowerLeft() {
    return new Vector3f(lowerLeft);
  }

  public Vector3f getLowerRight() {
    return new Vector3f(lowerRight);
  }

  public Vector3f getUpperLeft() {
    return new Vector3f(upperLeft);
  }

  public Vector3f getUpperRight() {
    return new Vector3f(upperRight);
  }

  public Vector2f getRotation() {
    Vector3 weNormal = Vector3.at(normal.x, normal.y, normal.z);

    float yaw = (float) weNormal.toYaw();
    float pitch = (float) weNormal.toPitch();

    return new Vector2f(yaw, pitch);
  }

  public Vector2f getDimensions() {
    return new Vector2f(width, height);
  }

  public Vector3f normal() {
    return new Vector3f(normal);
  }

  public Vector3f center() {
    return new Vector3f(center);
  }

  /**
   * Applies a transformation to this screen
   * @param matrix Matrix to apply
   */
  public void apply(Matrix4f matrix) {
    lowerLeft.sub(center).mulPosition(matrix).add(center);
    lowerRight.sub(center).mulPosition(matrix).add(center);
    upperLeft.sub(center).mulPosition(matrix).add(center);
    upperRight.sub(center).mulPosition(matrix).add(center);

    calculateDerivedValues();
  }

  /**
   * Translate screen coordinates in range [0...{@link #getDimensions()}] to a world position
   * @param screenPoint Screen point, with axes in range [0..{@link #getDimensions()}]
   * @param out Output value holder
   */
  public void screenToWorld(Vector2f screenPoint, Vector3f out) {
    Vector2f in = new Vector2f();
    screenToScreenspace(screenPoint, in);
    screenspaceToWorld(in, out);
  }

  /**
   * Converts screen coordinates in range [0..{@link #getDimensions()}) to a screen
   * space coordinate in range [0..1]
   *
   * @param in Screen input position
   * @param out Vector to store the value in
   */
  public void screenToScreenspace(Vector2f in, Vector2f out) {
    out.set(in).div(width, height);
  }

  /**
   * Converts screen space coordinates in range [0..1] to a screen coordinate in range
   * [0..{@link #getDimensions()})
   *
   * @param in Screen space input position
   * @param out Vector to store the value in
   */
  public void screenspaceToScreen(Vector2f in, Vector2f out) {
    out.set(in).mul(width, height);
  }

  /**
   * Translate screen space coordinates in range [0..1] to a world position.
   * @param screenPoint Screen point, with axes in range [0..1]
   * @param out Output value holder
   */
  public void screenspaceToWorld(Vector2f screenPoint, Vector3f out) {
    Vector3f height = new Vector3f(upperLeft).sub(lowerLeft);
    Vector3f width = new Vector3f(lowerRight).sub(lowerLeft);

    height.mul(screenPoint.y);
    width.mul(screenPoint.x);

    out.set(lowerLeft);
    out.add(width);
    out.add(height);
  }

  public boolean castRay(RayScan scan, Vector3f hitOut, Vector2f screenOut) {
    boolean didHit = planeIntersection(scan, hitOut);

    if (!didHit) {
      return false;
    }

    screenHitPoint(hitOut, screenOut);

    return (screenOut.x >= 0 && screenOut.x <= 1)
        && (screenOut.y >= 0 && screenOut.y <= 1);
  }

  private void screenHitPoint(Vector3f hitPoint, Vector2f out) {
    Vector3f height = new Vector3f(upperLeft).sub(lowerLeft);
    Vector3f width = new Vector3f(lowerRight).sub(lowerLeft);

    Vector3f relativePoint = new Vector3f(hitPoint).sub(lowerLeft);

    float x = relativePoint.dot(width) / width.lengthSquared();
    float y = relativePoint.dot(height) / height.lengthSquared();

    out.set(x, y);
  }

  private boolean planeIntersection(RayScan scan, Vector3f out) {
    float t = Intersectionf.intersectRayPlane(
        scan.getOrigin(),
        scan.getDirection(),
        center,
        normal,
        EPSILON
    );

    if (t < 0) {
      return false;
    }

    scan.getDirection().mul(t, out);
    out.add(scan.getOrigin());

    return true;
  }

  private void calculateDerivedValues() {
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();

    upperLeft.sub(lowerLeft, v1);
    lowerRight.sub(lowerLeft, v2);

    v2.cross(v1, normal);
    normal.normalize();

    Vector3f min = new Vector3f(lowerLeft);
    Vector3f max = new Vector3f(lowerLeft);

    min.min(lowerRight);
    min.min(upperLeft);
    min.min(upperRight);

    max.max(lowerRight);
    max.max(upperLeft);
    max.max(upperRight);

    center.set(min).add(max).mul(0.5f);

    calculateDimensions();
  }

  private void calculateDimensions() {
    Vector3f height = new Vector3f(upperLeft).sub(lowerLeft);
    Vector3f width = new Vector3f(upperRight).sub(upperLeft);
    this.width = width.length();
    this.height = height.length();
  }

}
