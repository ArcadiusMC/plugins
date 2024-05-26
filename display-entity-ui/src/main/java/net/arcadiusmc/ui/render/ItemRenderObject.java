package net.arcadiusmc.ui.render;

import org.bukkit.inventory.ItemStack;
import org.w3c.dom.Node;

class ItemRenderObject extends RenderObject {

  ItemStack itemStack;

  public ItemRenderObject(Node node, DocumentRender render) {
    super(node, render);
  }

  @Override
  <C, R> R visit(Visitor<C, R> visitor, C context) {
    return visitor.visitItem(this, context);
  }
}
