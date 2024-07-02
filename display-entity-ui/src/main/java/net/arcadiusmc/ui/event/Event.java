package net.arcadiusmc.ui.event;

import lombok.Getter;
import net.arcadiusmc.ui.struct.Node;

@Getter
public class Event {

  /** Flag to indicate that an event can be cancelled */
  public static final int FLAG_CANCELLABLE = 0x1;

  /** Flag to indicate if an event was cancelled */
  public static final int FLAG_CANCELLED = 0x2;

  /** Flag to indicate if propagation to other listeners was stopped */
  public static final int FLAG_PROPAGATION_STOPPED = 0x4;

  /** Flag to indicate if the event should be propagated to parent nodes */
  public static final int FLAG_BUBBLING = 0x8;

  private final String eventType;
  private final Node target;

  int flags;

  public Event(String eventType, Node target) {
    this.eventType = eventType;
    this.target = target;
  }

  public boolean isCancellable() {
    return (flags & FLAG_CANCELLABLE) == FLAG_CANCELLABLE;
  }

  public boolean isCancelled() {
    return (flags & FLAG_CANCELLED) == FLAG_CANCELLED;
  }

  public boolean propagationStopped() {
    return (flags & FLAG_PROPAGATION_STOPPED) == FLAG_PROPAGATION_STOPPED;
  }

  public boolean isBubbling() {
    return (flags & FLAG_BUBBLING) == FLAG_BUBBLING;
  }

  public void stopPropagation() {
    flags |= FLAG_PROPAGATION_STOPPED;
  }

  public void preventDefault() {
    if (!isCancellable()) {
      return;
    }

    flags |= FLAG_CANCELLED;
  }
}
