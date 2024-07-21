package net.arcadiusmc.ui.event;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ui.struct.Document;
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
  private final Document view;

  @Getter @Setter
  private EventTarget currentTarget;

  int flags;

  public Event(String eventType, Document view, Node target) {
    this.eventType = eventType;
    this.target = target;
    this.view = view;
  }

  public final boolean isCancellable() {
    return hasFlag(FLAG_CANCELLABLE);
  }

  public final boolean isCancelled() {
    return hasFlag(FLAG_CANCELLED);
  }

  public final boolean propagationStopped() {
    return hasFlag(FLAG_PROPAGATION_STOPPED);
  }

  public final boolean isBubbling() {
    return hasFlag(FLAG_BUBBLING);
  }

  public final void stopPropagation() {
    flags |= FLAG_PROPAGATION_STOPPED;
  }

  private boolean hasFlag(int mask) {
    return (this.flags & mask) == mask;
  }

  public final void preventDefault() {
    if (!isCancellable()) {
      return;
    }

    flags |= FLAG_CANCELLED;
  }
}
