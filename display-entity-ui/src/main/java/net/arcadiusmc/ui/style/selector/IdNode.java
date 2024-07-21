package net.arcadiusmc.ui.style.selector;

import java.util.Objects;
import net.arcadiusmc.ui.struct.Attr;
import net.arcadiusmc.ui.struct.Element;

public record IdNode(String id) implements SelectorNode {

  @Override
  public boolean test(Element e) {
    String elementId = e.getAttribute(Attr.ID);
    return Objects.equals(id, elementId);
  }

  @Override
  public void append(StringBuilder builder) {
    builder.append("#").append(id);
  }

  @Override
  public void appendSpecificity(Spec spec) {
    spec.idColumn++;
  }
}
