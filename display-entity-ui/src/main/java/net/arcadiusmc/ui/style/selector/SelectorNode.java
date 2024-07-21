package net.arcadiusmc.ui.style.selector;

import net.arcadiusmc.ui.struct.Element;

public interface SelectorNode {

  SelectorNode MATCH_ALL = MatchAll.MATCH_ALL;

  boolean test(Element node);

  void append(StringBuilder builder);

  void appendSpecificity(Spec spec);

  enum MatchAll implements SelectorNode {
    MATCH_ALL;

    @Override
    public boolean test(Element node) {
      return true;
    }

    @Override
    public void append(StringBuilder builder) {
      builder.append("*");
    }

    @Override
    public void appendSpecificity(Spec spec) {

    }
  }
}
