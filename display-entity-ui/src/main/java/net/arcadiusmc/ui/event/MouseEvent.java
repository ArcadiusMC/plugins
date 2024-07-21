package net.arcadiusmc.ui.event;

import lombok.Getter;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.struct.Node;
import org.joml.Vector2f;
import org.joml.Vector3f;

@Getter
public class MouseEvent extends Event {

  private final boolean shiftPressed;
  private final MouseButton button;

  private final Vector2f screenPosition;
  private final Vector3f worldPosition;

  public MouseEvent(
      String eventType,
      boolean shiftPressed,
      Vector2f screenPosition,
      Vector3f worldPosition,
      MouseButton button,
      Node relatedTarget,
      int flags,
      Document view
  ) {
    super(eventType, view, relatedTarget);

    this.shiftPressed = shiftPressed;
    this.button = button;

    this.screenPosition = screenPosition;
    this.worldPosition = worldPosition;

    this.flags |= flags;
  }

  public enum MouseButton {
    NONE,
    LEFT,
    RIGHT
  }
}
