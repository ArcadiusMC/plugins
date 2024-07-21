package net.arcadiusmc.ui.style.selector;

import java.util.Objects;
import net.arcadiusmc.ui.struct.Element;

public record TagNameNode(String tagName) implements SelectorNode {

  @Override
  public boolean test(Element e) {
    return Objects.equals(e.getTagName(), tagName);
  }

  @Override
  public void append(StringBuilder builder) {
    builder.append(tagName);
  }

  @Override
  public void appendSpecificity(Spec spec) {
    spec.typeColumn++;
  }
}
