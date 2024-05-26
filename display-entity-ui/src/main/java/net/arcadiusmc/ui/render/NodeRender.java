package net.arcadiusmc.ui.render;

import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector2f;
import org.w3c.dom.Node;

class NodeRender {

  private final Node node;
  private final DocumentRender render;

  private final Vector2f min = new Vector2f();
  private final Vector2f max = new Vector2f();

  TextDisplay border;
  TextDisplay background;
  Display entity;

  public NodeRender(Node node, DocumentRender render) {
    this.node = node;
    this.render = render;
  }


}
