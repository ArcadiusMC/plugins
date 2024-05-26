package net.arcadiusmc.ui.render;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import net.arcadiusmc.ui.PageView;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DocumentRender {

  static final String ITEM_ELEMENT = "item";

  /**
   * The amount you have to scale an empty text display entity up for it to cover a whole block
   */
  static final float EMPTY_TEXT_SIZE_TO_BLOCK = 40;

  /**
   * Size of an empty, unscaled text display
   */
  static final float EMPTY_TEXT_SIZE = 1 / EMPTY_TEXT_SIZE_TO_BLOCK;

  private final PageView view;

  private final Map<Node, RenderObject> map = new HashMap<>();

  @Getter
  private final DisplayEntityPool pool;

  public DocumentRender(PageView view) {
    this.view = view;
    this.pool = new DisplayEntityPool(view.getWorld());
  }

  public RenderObject getRender(Node node) {
    RenderObject render = map.get(node);

    if (render != null) {
      return render;
    }

    if (node instanceof Element element) {
      if (element.getTagName().equals(ITEM_ELEMENT)) {
        render = new ItemRenderObject(node, this);
      } else {
        render = new TextRenderObject(node, this);
      }
    } else {
      render = new TextRenderObject(node, this);
    }

    map.put(node, render);

    return render;
  }

  public void layout() {

  }

  public void render() {

  }
}
