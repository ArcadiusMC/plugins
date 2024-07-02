package net.arcadiusmc.ui.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.ui.HideUtil;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.event.Event;
import net.arcadiusmc.ui.event.EventListeners;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import org.bukkit.entity.Display;
import org.joml.Vector2f;
import org.joml.Vector4f;

@Getter
public class Node {

  private final EventListeners listeners;

  private int depth = 0;
  private Node parent;
  private final List<Node> children = new ArrayList<>();

  private final RenderElement renderElement = new RenderElement();

  private int flags;
  private boolean hidden;

  private AlignDirection direction = AlignDirection.Y;

  public Node(EventListeners listeners) {
    this.listeners = listeners;
  }

  public void fireEvent(Event event) {
    if (!event.isBubbling()) {
      listeners.fireEvent(event);
      return;
    }

    Node p = this;

    while (p != null) {
      p.listeners.fireEvent(event);

      if (event.propagationStopped()) {
        return;
      }

      p = p.parent;
    }
  }

  public boolean hasFlags(NodeFlag... flags) {
    int mask = NodeFlag.combineMasks(flags);
    return (this.flags & mask) == mask;
  }

  public void addFlags(NodeFlag... flags) {
    int mask = NodeFlag.combineMasks(flags);
    this.flags |= mask;
  }

  public void removeFlags(NodeFlag... flags) {
    int mask = NodeFlag.combineMasks(flags);
    this.flags = this.flags & ~mask;
  }

  public void addChild(Node node) {
    if (node.parent != null) {
      throw new IllegalStateException("Child already has parent");
    }

    node.parent = this;
    node.setDepth(depth + 1);

    children.add(node);
  }

  public void setDepth(int depth) {
    this.depth = depth;
    this.renderElement.setDepth(depth);

    for (Node child : children) {
      child.setDepth(depth + 1);
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

    boolean aligningOnX = direction == AlignDirection.X;

    Vector2f alignPos = new Vector2f();
    renderElement.getAlignmentPosition(alignPos, direction);

    Vector2f pos = new Vector2f(alignPos);
    Vector2f tempMargin = new Vector2f(0);
    Vector2f elemSize = new Vector2f();

    Vector2f maxPos = new Vector2f(Float.MIN_VALUE, Float.MAX_VALUE);
    Vector2f childMax = new Vector2f();

    for (Node child : children) {
      RenderElement render = child.getRenderElement();
      Vector4f margin = render.getMarginSize();

      if (aligningOnX) {
        pos.x += margin.x;

        tempMargin.set(0, -margin.y);
        pos.y -= margin.y;
      } else {
        pos.y -= margin.y;

        tempMargin.set(margin.x, 0);
        pos.x += margin.x;
      }

      child.moveTo(view, pos);
      pos.sub(tempMargin);

      render.getElementSize(elemSize);

      if (aligningOnX) {
        pos.x += margin.w + elemSize.x;
      } else {
        pos.y -= margin.z + elemSize.y;
      }

      render.getElementSize(childMax);
      childMax.add(render.getPosition());
      childMax.x += margin.w;
      childMax.y -= margin.z;

      maxPos.x = Math.max(childMax.x, maxPos.x);
      maxPos.y = Math.min(childMax.y, maxPos.y);
    }

    if (hasFlags(NodeFlag.ROOT)) {
      return;
    }

    Vector2f contentMax = new Vector2f();
    renderElement.getContentEnd(contentMax);

    Vector2f dif = new Vector2f();
    dif.x = Math.max(maxPos.x - contentMax.x, 0);
    dif.y = Math.max(contentMax.y - maxPos.y, 0);

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

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
    this.renderElement.setHidden(hidden);

    if (hidden) {
      forEachEntity(HideUtil::hide);
    } else {
      forEachEntity(HideUtil::unhide);
    }
  }

  public void setDirection(AlignDirection direction) {
    Objects.requireNonNull(direction, "Null direction");
    this.direction = direction;
  }
}
