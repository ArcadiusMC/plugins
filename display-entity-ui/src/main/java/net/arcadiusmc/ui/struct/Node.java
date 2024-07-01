package net.arcadiusmc.ui.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import org.bukkit.entity.Display;
import org.joml.Vector2f;
import org.joml.Vector4f;

@Getter
public class Node {

  @Setter
  private int depth = 0;
  private Node parent;
  private final List<Node> children = new ArrayList<>();

  private final RenderElement renderElement = new RenderElement();

  public void addChild(Node node) {
    if (node.parent != null) {
      throw new IllegalStateException("Child already has parent");
    }

    node.parent = this;
    node.updateDepth(depth + 1);

    children.add(node);
  }

  private void updateDepth(int depth) {
    this.depth = depth;
    this.renderElement.setDepth(depth);

    for (Node child : children) {
      child.updateDepth(depth + 1);
    }
  }

  public void spawn(PageView view) {
    for (Node child : children) {
      child.spawn(view);
    }

    renderElement.spawn(view);
  }

  public void align(PageView view) {
    if (children.isEmpty()) {
      return;
    }

    for (Node child : children) {
      child.align(view);
    }

    final Vector2f pos = new Vector2f();
    final Vector2f elSize = new Vector2f();

    renderElement.getAlignmentPosition(pos);

    for (Node child : children) {
      Vector4f margin = child.getRenderElement().getMarginSize();

      pos.sub(0, margin.y);
      pos.add(margin.x, 0);

      child.moveTo(view, pos);
      child.getRenderElement().getElementSize(elSize);
      pos.sub(0, elSize.y);

      pos.sub(margin.x, 0);
      pos.sub(0, margin.z);
    }

    Vector2f elPos = renderElement.getPosition();
    Vector2f dif = new Vector2f(elPos).sub(pos);

    renderElement.getContentExtension().set(dif);
  }

  public void moveTo(PageView view, Vector2f pos) {
    Vector2f currentPos = new Vector2f(renderElement.getPosition());
    renderElement.moveTo(view, pos);

    if (children.isEmpty()) {
      return;
    }

    final Vector2f newChildPos = new Vector2f();
    final Vector2f currentChildPos = new Vector2f();
    final Vector2f dif = new Vector2f();

    for (Node child : children) {
      currentChildPos.set(child.renderElement.getPosition());
      dif.set(currentChildPos).sub(currentPos);

      newChildPos.set(pos).add(dif);
      child.moveTo(view, newChildPos);
    }
  }

  public void forEachEntity(Consumer<Display> op) {
    for (Layer layer : renderElement.getLayers()) {
      if (layer == null || !layer.isSpawned()) {
        continue;
      }

      op.accept(layer.getEntity());
    }

    for (Node child : children) {
      child.forEachEntity(op);
    }
  }
}
