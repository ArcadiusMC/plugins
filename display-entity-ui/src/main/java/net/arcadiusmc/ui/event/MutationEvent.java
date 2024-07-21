package net.arcadiusmc.ui.event;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.struct.Node;

@Getter @Setter
public class MutationEvent extends Event {

  private Node node;
  private Action action;

  private String attrName;
  private String prevValue;
  private String newValue;

  public MutationEvent(String eventType, Document view, Node target) {
    super(eventType, view, target);
    this.flags |= FLAG_BUBBLING | FLAG_CANCELLABLE;
  }

  public enum Action {
    APPEND,
    REMOVE,
    SET_ATTR,
    ADD_ATTR,
    REMOVE_ATTR;
  }
}
