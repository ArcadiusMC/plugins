package net.arcadiusmc.ui.style.selector;

import net.arcadiusmc.ui.struct.ButtonElement;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.struct.NodeFlag;

public record PseudoClassNode(SelectorNode base, PseudoClass pseudoClass) implements SelectorNode {

  @Override
  public boolean test(Element node) {
    if (!base.test(node)) {
      return false;
    }

    return switch (pseudoClass) {
      case ACTIVE -> node.hasFlags(NodeFlag.CLICKED);
      case HOVER -> node.hasFlags(NodeFlag.HOVERED);

      case DISABLED -> {
        if (node instanceof ButtonElement button) {
          yield !button.isEnabled();
        }

        yield false;
      }
      case ENABLED -> {
        if (node instanceof ButtonElement button) {
          yield button.isEnabled();
        }

        yield false;
      }
    };
  }

  @Override
  public void append(StringBuilder builder) {
    base.append(builder);

    builder.append(':');

    switch (pseudoClass) {
      case HOVER -> builder.append("hover");
      case ACTIVE -> builder.append("active");
      case DISABLED -> builder.append("disabled");
      case ENABLED -> builder.append("enabled");
    }
  }

  @Override
  public void appendSpecificity(Spec spec) {
    base.appendSpecificity(spec);
    spec.classColumn++;
  }

  public enum PseudoClass {
    ACTIVE,
    HOVER,
    DISABLED,
    ENABLED;
  }
}
