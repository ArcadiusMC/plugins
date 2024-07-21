package net.arcadiusmc.ui.struct;

import com.google.common.base.Strings;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ui.event.EventTypes;
import net.arcadiusmc.ui.event.MutationEvent;
import net.arcadiusmc.ui.event.MutationEvent.Action;
import net.arcadiusmc.ui.util.StringUtil;

@Getter
public class Element extends Node {

  private final Map<String, String> attributes = new HashMap<>();
  private final String tagName;

  @Setter
  private Node hoverNode;

  public Element(Document owning, String tagName) {
    super(owning);
    this.tagName = tagName;
  }

  public Node getTooltip() {
    return hoverNode;
  }

  public void setAttribute(String attribute, String value) {
    Objects.requireNonNull(attribute, "Null attribute");
    String previousValue = attributes.get(attribute);

    if (Strings.isNullOrEmpty(previousValue) && Strings.isNullOrEmpty(value)) {
      return;
    }
    if (Objects.equals(previousValue, value)) {
      return;
    }

    attributes.put(attribute, value);

    MutationEvent event = new MutationEvent(EventTypes.MODIFY_ATTR, getOwning(), this);

    event.setNode(this);
    event.setAttrName(attribute);
    event.setNewValue(Strings.nullToEmpty(value));
    event.setPrevValue(Strings.nullToEmpty(previousValue));

    if (Strings.isNullOrEmpty(value)) {
      event.setAction(Action.REMOVE_ATTR);
    } else if (Strings.isNullOrEmpty(previousValue)) {
      event.setAction(Action.ADD_ATTR);
    } else {
      event.setAction(Action.SET_ATTR);
    }

    dispatchEvent(event);
  }

  public String getAttribute(String attribute) {
    return attributes.get(attribute);
  }

  public boolean getBooleanAttribute(String attribute, boolean defaultValue) {
    String val = getAttribute(attribute);

    if (Strings.isNullOrEmpty(val)) {
      return defaultValue;
    }

    Boolean b = StringUtil.parseBoolean(attribute);

    if (b == null) {
      return defaultValue;
    }

    return b;
  }

  public Set<String> getAttributes() {
    return Collections.unmodifiableSet(attributes.keySet());
  }

  @Override
  public void visitorEnter(Visitor visitor) {
    visitor.enterElement(this);
  }

  @Override
  public void visitorExit(Visitor visitor) {
    visitor.exitElement(this);
  }

  private String attributesToString() {
    if (attributes.isEmpty()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();

    attributes.forEach((s, s2) -> {
      builder
          .append(' ')
          .append(s)
          .append('=')
          .append('"')
          .append(s2)
          .append('"');
    });

    return builder.toString();
  }

  @Override
  public String toString() {
    return "<" + tagName + attributesToString() + " />";
  }
}
