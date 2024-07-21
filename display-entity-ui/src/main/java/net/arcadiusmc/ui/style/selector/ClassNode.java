package net.arcadiusmc.ui.style.selector;

import com.google.common.base.Strings;
import net.arcadiusmc.ui.struct.Attr;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.util.StringUtil;

public record ClassNode(String className) implements SelectorNode {

  @Override
  public boolean test(Element e) {
    String classList = e.getAttribute(Attr.CLASS);

    if (Strings.isNullOrEmpty(classList)) {
      return false;
    }

    return StringUtil.containsWord(classList, className);
  }

  @Override
  public void append(StringBuilder builder) {
    builder.append('.').append(className);
  }

  @Override
  public void appendSpecificity(Spec spec) {
    spec.classColumn++;
  }
}
