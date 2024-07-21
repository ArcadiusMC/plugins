package net.arcadiusmc.ui.style.selector;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import java.util.List;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.util.StringUtil;

public class AttributedNode implements SelectorNode {

  private final SelectorNode base;
  private final List<AttributeTest> attributeTests;

  public AttributedNode(SelectorNode base, List<AttributeTest> attributeTests) {
    this.base = base;
    this.attributeTests = attributeTests;
  }

  @Override
  public void appendSpecificity(Spec spec) {
    base.appendSpecificity(spec);
    spec.classColumn += attributeTests.size();
  }

  @Override
  public boolean test(Element element) {
    if (!base.test(element)) {
      return false;
    }

    for (AttributeTest attributeTest : attributeTests) {
      if (!attributeTest.test(element)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void append(StringBuilder builder) {
    base.append(builder);

    if (attributeTests.isEmpty()) {
      return;
    }

    for (AttributeTest attributeTest : attributeTests) {
      attributeTest.append(builder);
    }
  }

  public record AttributeTest(String attrName, Operation op, String value) {

    public void append(StringBuilder builder) {
      builder.append("[");
      builder.append(attrName);

      switch (op) {
        case DASH_PREFIXED:
          builder.append("|=");
          break;

        case CONTAINS_WORD:
          builder.append("~=");
          break;

        case STARTS_WITH:
          builder.append("^=");
          break;

        case ENDS_WITH:
          builder.append("$=");
          break;

        case EQUALS:
          builder.append("=");
          break;

        case CONTAINS_SUBSTRING:
          builder.append("*=");
          break;

        default:
          break;
      }

      if (!Strings.isNullOrEmpty(value)) {
        builder.append('"').append(value).append('"');
      }

      builder.append(']');
    }

    public boolean test(Element element) {
      String attrValue = element.getAttribute(attrName);

      if (Strings.isNullOrEmpty(attrValue)) {
        return op == Operation.HAS;
      }

      return switch (op) {
        case HAS -> true;
        case EQUALS -> Objects.equal(value, attrValue);
        case ENDS_WITH -> attrValue.endsWith(value);
        case STARTS_WITH -> attrValue.startsWith(value);
        case DASH_PREFIXED -> attrValue.startsWith(value) || attrValue.startsWith(value + "-");
        case CONTAINS_SUBSTRING -> attrValue.contains(value);
        case CONTAINS_WORD -> StringUtil.containsWord(attrValue, value);
      };
    }
  }

  public enum Operation {
    HAS,
    EQUALS,
    ENDS_WITH,
    STARTS_WITH,
    DASH_PREFIXED,
    CONTAINS_SUBSTRING,
    CONTAINS_WORD;
  }
}
