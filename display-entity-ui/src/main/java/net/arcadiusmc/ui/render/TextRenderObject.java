package net.arcadiusmc.ui.render;

import org.w3c.dom.Node;

class TextRenderObject extends RenderObject {

  String text = "";

  public TextRenderObject(Node node, DocumentRender render) {
    super(node, render);
  }

  @Override
  <C, R> R visit(Visitor<C, R> visitor, C context) {
    return visitor.visitText(this, context);
  }
}
