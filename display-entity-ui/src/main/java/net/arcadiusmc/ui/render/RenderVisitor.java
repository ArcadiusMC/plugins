package net.arcadiusmc.ui.render;

import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class RenderVisitor implements Visitor<Void, Void> {

  final DocumentRender render;
  final DisplayEntityPool pool;

  public RenderVisitor(DocumentRender render) {
    this.render = render;
    this.pool = render.getPool();
  }

  @Override
  public Void visitText(TextRenderObject object, Void unused) {
    if (!Text.isEmpty(object.text)) {
      TextDisplay display = pool.getTextDisplay();
      display.text(object.text);
      object.setSublayer(NodeEntity.MAIN, display);
    }

    genericRender(object);
    return null;
  }

  @Override
  public Void visitItem(ItemRenderObject object, Void unused) {
    if (!ItemStacks.isEmpty(object.itemStack)) {
      ItemDisplay display = pool.getItemDisplay();
      display.setItemStack(object.itemStack);
      object.setSublayer(NodeEntity.MAIN, display);
    }

    genericRender(object);
    return null;
  }

  void genericRender(RenderObject object) {
    NodeList nodes = object.node.getChildNodes();

    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      RenderObject renderObject = render.getRender(node);
      renderObject.visit(this, null);
    }
  }
}
