package net.arcadiusmc.ui.event;

import lombok.Getter;
import net.arcadiusmc.ui.struct.Node;
import org.joml.Vector2f;
import org.joml.Vector3f;

@Getter
public class MouseEvent extends Event {

  private final boolean shiftPressed;
  private final Vector2f screenPosition;
  private final Vector3f worldPosition;

  public MouseEvent(
      String eventType,
      boolean shiftPressed,
      Vector2f screenPosition,
      Vector3f worldPosition,
      Node relatedTarget
  ) {
    super(eventType, relatedTarget);

    this.shiftPressed = shiftPressed;

    this.screenPosition = screenPosition;
    this.worldPosition = worldPosition;
  }
}
