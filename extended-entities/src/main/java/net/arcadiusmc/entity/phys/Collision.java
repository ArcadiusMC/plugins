package net.arcadiusmc.entity.phys;

import com.badlogic.ashley.core.Entity;
import lombok.Getter;

@Getter
public class Collision<T> {

  private final Entity entity;
  private final T collided;
  private final AxisAlignedBounds collisionArea;
  private final Shape entityShape;

  public Collision(Entity entity, T collided, AxisAlignedBounds collisionArea, Shape entityShape) {
    this.entity = entity;
    this.collided = collided;
    this.collisionArea = collisionArea;
    this.entityShape = entityShape;
  }
}
