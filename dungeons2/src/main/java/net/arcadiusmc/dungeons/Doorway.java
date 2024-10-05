package net.arcadiusmc.dungeons;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class Doorway {

  private static final Logger LOGGER = Loggers.getLogger();

  private Direction direction = Direction.NORTH;
  private Vector3i center = Vector3i.ZERO;
  private Opening opening = Opening.DEFAULT;

  private DungeonPiece from;
  private Doorway to;

  private boolean stairs;
  private boolean entrance;

  public Doorway() {

  }

  public Doorway(Direction direction, Vector3i center, DungeonPiece from) {
    this.direction = direction;
    this.center = center;
    this.from = from;
  }

  public void transform(Transform transform) {
    direction = direction.rotate(transform.getRotation());
    center = transform.apply(center);

    if (to == null || entrance) {
      return;
    }

    to.transform(transform);
  }

  public Vector3i rightSide() {
    Direction right = direction.right();
    int halfWidth = opening.width() / 2;

    return center
        .add(right.getMod().mul(halfWidth, 0, halfWidth))
        .sub(direction.getMod());
  }

  public void connect(Doorway entrance) {
    if (entrance.getFrom() == from) {
      throw new IllegalStateException("entrance.getFrom() == this.from");
    }
    if (entrance.getTo() != null) {
      throw new IllegalStateException("entrance.to != null");
    }
    if (isEntrance()) {
      throw new IllegalStateException("parent connector");
    }
    if (entrance.isEntrance()) {
      throw new IllegalStateException("Already entrance");
    }
    if (entrance.getFrom().getEntrance() != null) {
      throw new IllegalStateException("entrance already has parent connector");
    }
    for (var d: from.getDoorways()) {
      if (d.to == entrance) {
        throw new IllegalStateException("Already set on other doorway");
      }
    }

    align(entrance);

    entrance.testForSelfReference("entrance::before connect");
    testForSelfReference("before connect");

    this.to = entrance;

    entrance.to = this;
    entrance.entrance = true;

    try {
      onDepthPropagate(from.getDepth() + 1);
    } catch (StackOverflowError err) {
      LOGGER.error("Stack overflow error in connect(): ", new Throwable("Stack trace"));
    }

    testForSelfReference("after connect");
    entrance.testForSelfReference("entrance::after connect");
  }

  public void disconnect() {
    if (this.to == null) {
      return;
    }

    testForSelfReference("before disconnect");

    this.to.entrance = false;
    this.to.to = null;
    this.to = null;

    testForSelfReference("after disconnect");
  }

  private void testForSelfReference(String when) {
    try {
      from.getRoot();
      from.testSelfReference();
    } catch (StackOverflowError err) {
      throw new IllegalStateException("Self reference has occurred " + when);
    }

    if (to == null) {
      return;
    }

    try {
      to.from.getRoot();
      to.from.testSelfReference();
    } catch (StackOverflowError error) {
      throw new IllegalStateException("Self reference has occurred " + when);
    }
  }

  // Align the other gateway to this gateway
  //
  // this = exit
  // other = entrance
  //
  public void align(Doorway entrance) {
    old_align(entrance);
  }

  private void old_align(Doorway otherEntrance) {
    Vector3i right = rightSide();

    Rotation rotation = otherEntrance.getDirection()
        .deriveRotationFrom(direction)
        .add(Rotation.CLOCKWISE_180);

    Vector3i relativeEntranceOrigin = otherEntrance.getDirection()
        .left()
        .getMod()
        .mul(opening.width(), 0, opening.width())
        .div(2);

    Vector3i entranceLeft = otherEntrance.getCenter()
        .add(relativeEntranceOrigin);

    DungeonPiece other = otherEntrance.getFrom();

    if (rotation != Rotation.NONE) {
      other.transform(Transform.rotation(rotation));
      entranceLeft = rotation.rotate(entranceLeft);
    }

    other.transform(
        Transform.offset(right.sub(entranceLeft))
            .withIPivot(other.getPivotPoint())
    );
  }

  void onDepthPropagate(int depth) {
    if (entrance || to == null) {
      return;
    }

    to.from.setDepth(depth);
  }
}
