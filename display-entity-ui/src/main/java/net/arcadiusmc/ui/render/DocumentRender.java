package net.arcadiusmc.ui.render;

import java.util.HashMap;
import java.util.Map;
import net.arcadiusmc.ui.PageView;
import org.w3c.dom.Node;

public class DocumentRender {

  /**
   * The amount you have to scale an empty text display entity up for it to cover a whole block
   */
  static final float EMPTY_TEXT_SIZE_TO_BLOCK = 40;

  /**
   * Size of an empty, unscaled text display
   */
  static final float EMPTY_TEXT_SIZE = 1 / EMPTY_TEXT_SIZE_TO_BLOCK;

  private final PageView view;

  private final Map<Node, NodeRender> map = new HashMap<>();

  public DocumentRender(PageView view) {
    this.view = view;
  }

  public NodeRender getRender(Node node) {
    NodeRender render = map.get(node);

    if (render != null) {
      return render;
    }

    render = new NodeRender(node, this);
    map.put(node, render);

    return render;
  }


}
