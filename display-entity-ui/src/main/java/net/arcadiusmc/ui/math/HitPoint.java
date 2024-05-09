package net.arcadiusmc.ui.math;

import java.util.Objects;
import lombok.Getter;
import org.joml.Vector2f;
import org.joml.Vector3f;

@Getter
public class HitPoint {
  private Vector3f worldHitPoint;
  private Vector2f screenHit;

  public HitPoint(Vector3f worldHitPoint, Vector2f screenHit) {
    Objects.requireNonNull(worldHitPoint, "Null worldHitPoint");
    Objects.requireNonNull(screenHit, "Null screenHit");

    this.worldHitPoint = worldHitPoint;
    this.screenHit = screenHit;
  }
}
