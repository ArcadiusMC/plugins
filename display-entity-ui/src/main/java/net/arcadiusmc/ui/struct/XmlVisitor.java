package net.arcadiusmc.ui.struct;

import java.util.Set;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.style.Rule;
import net.arcadiusmc.ui.style.StylePropertyMap;
import net.arcadiusmc.ui.style.StylePropertyMap.RuleIterator;
import org.joml.Vector2f;

public class XmlVisitor implements Visitor {

  static final String COMMENT_START = "<!--";
  static final String COMMENT_END = "-->";

  private final StringBuilder builder = new StringBuilder();
  private int indent = 0;

  private StringBuilder nlIndent() {
    return builder.append('\n')
        .append("  ".repeat(indent));
  }

  private void addDebugComment(Node element) {
    nlIndent().append(COMMENT_START);
    indent++;

    RenderElement re = element.getRenderElement();
    nlIndent().append("render-element:");
    indent++;

    nlIndent().append("content-scale: ").append(re.getContentScale());
    nlIndent().append("content: ").append(re.getContent());

    Vector2f vector = new Vector2f();

    re.getElementSize(vector);
    nlIndent().append("size: ").append(vector);

    re.getContentStart(vector);
    nlIndent().append("content-start: ").append(vector);

    re.getContentEnd(vector);
    nlIndent().append("content-end: ").append(vector);

    nlIndent().append("content-dirty: ").append(re.isContentDirty());
    nlIndent().append("position: ").append(re.getPosition());
    nlIndent().append("max-size: ").append(re.getMaxSize());
    nlIndent().append("min-size: ").append(re.getMinSize());
    nlIndent().append("padding: ").append(re.getPaddingSize());
    nlIndent().append("outline-size: ").append(re.getOutlineSize());
    nlIndent().append("content-ext: ").append(re.getContentExtension());
    nlIndent().append("depth: ").append(re.getDepth());

    indent--;

    builder.append("\n");
    nlIndent().append("layers:");

    indent++;

    for (Layer layer : re.getLayers()) {
      if (RenderElement.isNotSpawned(layer)) {
        continue;
      }

      nlIndent().append("layer[").append(layer.getLayer()).append("]:");
      indent++;

      nlIndent().append("size: ").append(layer.getSize());
      nlIndent().append("border-size: ").append(layer.getBorderSize());
      nlIndent().append("depth: ").append(layer.getDepth());
      nlIndent().append("scale: ").append(layer.getScale());
      nlIndent().append("translate: ").append(layer.getTranslate());

      indent--;
    }

    indent--;

    StylePropertyMap set = element.getRenderElement().getStyleProperties();
    RuleIterator it = set.iterator();

    if (it.hasNext()) {
      builder.append('\n');
      nlIndent().append("rules:");
      indent++;

      while (it.hasNext()) {
        it.next();

        Rule<Object> rule = it.rule();
        Object value = it.value();

        nlIndent()
            .append(rule.getKey())
            .append(" = ")
            .append(value);
      }

      indent--;
    }

    indent--;
    nlIndent().append(COMMENT_END);
  }

  @Override
  public void enterElement(Element element) {
    nlIndent().append('<').append(element.getTagName());

    Set<String> attrs = element.getAttributes();
    if (!attrs.isEmpty()) {

      for (String attr : attrs) {
        String val = element.getAttribute(attr);

        if (val == null) {
          continue;
        }
        builder.append(' ').append(attr).append("=").append('"').append(val).append('"');
      }
    }

    builder.append('>');
    indent++;

    addDebugComment(element);
  }

  @Override
  public void exitElement(Element element) {
    indent--;

    nlIndent()
        .append("</")
        .append(element.getTagName())
        .append('>');
  }

  @Override
  public void enterText(TextNode node) {
    nlIndent().append(node.getTextContent());
    addDebugComment(node);
  }

  @Override
  public void exitText(TextNode node) {

  }

  @Override
  public String toString() {
    return builder.toString();
  }
}
