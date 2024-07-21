package net.arcadiusmc.ui.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.event.Event;
import net.arcadiusmc.ui.event.EventExecutionTask;
import net.arcadiusmc.ui.event.EventListener;
import net.arcadiusmc.ui.event.EventListeners;
import net.arcadiusmc.ui.event.EventTarget;
import net.arcadiusmc.ui.event.EventTypes;
import net.arcadiusmc.ui.event.MutationEvent;
import net.arcadiusmc.ui.event.MutationEvent.Action;
import net.arcadiusmc.ui.math.Rect;
import net.arcadiusmc.ui.math.Rectangle;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.util.HideUtil;
import org.bukkit.entity.Display;
import org.joml.Vector2f;
import org.slf4j.Logger;

@Getter
public abstract class Node implements EventTarget {

  private static final Logger LOGGER = Loggers.getLogger();

  private final EventListeners listeners = new EventListeners();
  private final RenderElement renderElement;

  private int depth = 0;
  private Node parent = null;
  private final Document owning;
  private final List<Node> children = new ArrayList<>();

  private boolean hidden = false;
  private int flags = 0;
  private Align direction = Align.Y;
  private final Rect margin = new Rect();

  public Node(Document owning) {
    this.owning = owning;
    this.renderElement = new RenderElement(owning);
  }

  public boolean ignoredByMouse() {
    return false;
  }

  protected void updateRender() {
    if (!renderElement.isSpawned()) {
      return;
    }

    renderElement.spawn();
  }

  public boolean hasFlags(NodeFlag... flags) {
    int mask = NodeFlag.combineMasks(flags);
    return (this.flags & mask) == mask;
  }

  public void addFlag(NodeFlag flag) {
    this.flags |= flag.mask;
  }

  public void removeFlag(NodeFlag flag) {
    this.flags &= ~flag.mask;
  }

  public void addChild(Node node) {
    if (node.parent != null) {
      throw new IllegalStateException("Child already has parent");
    }

    MutationEvent e = new MutationEvent(EventTypes.APPEND_CHILD, getOwning(), this);
    e.setAction(Action.APPEND);
    e.setNode(node);

    dispatchEvent(e);

    if (e.isCancelled()) {
      return;
    }

    node.parent = this;
    node.setDepth(depth + 1);

    children.add(node);
  }

  public void removeChild(Node n) {
    int index = children.indexOf(n);
    if (index == -1) {
      return;
    }
    removeChildByIndex(index);
  }

  public void removeChildByIndex(int index) {
    Objects.checkIndex(index, children.size());
    Node n = children.get(index);

    MutationEvent event = new MutationEvent(EventTypes.REMOVE_CHILD, getOwning(), this);
    event.setAction(Action.REMOVE);
    event.setNode(n);

    dispatchEvent(event);

    if (event.isCancelled()) {
      return;
    }

    children.remove(index);
  }

  public void setDepth(int depth) {
    this.depth = depth;
    this.renderElement.setDepth(depth);

    for (Node child : children) {
      child.setDepth(depth + 1);
    }
  }

  public void spawn() {
    for (Node child : children) {
      child.spawn();
    }

    renderElement.spawn();
  }

  public void align() {
    if (children.isEmpty()) {
      return;
    }

    for (Node child : children) {
      child.align();
    }

    boolean aligningOnX = direction == Align.X;

    Vector2f alignPos = new Vector2f();
    renderElement.getAlignmentPosition(alignPos, direction);

    Vector2f pos = new Vector2f(alignPos);
    Vector2f tempMargin = new Vector2f(0);
    Vector2f elemSize = new Vector2f();

    for (Node child : children) {
      if (child.hidden) {
        continue;
      }

      RenderElement render = child.getRenderElement();
      Rect margin = child.margin;

      if (aligningOnX) {
        pos.x += margin.left;

        tempMargin.set(0, -margin.top);
        pos.y -= margin.top;
      } else {
        pos.y -= margin.top;

        tempMargin.set(margin.left, 0);
        pos.x += margin.left;
      }

      child.moveTo(pos);
      pos.sub(tempMargin);

      render.getElementSize(elemSize);

      if (aligningOnX) {
        pos.x += margin.right + elemSize.x;
      } else {
        pos.y -= margin.bottom + elemSize.y;
      }
    }

    postAlign();
  }

  void postAlign() {
    if (children.isEmpty()) {
      return;
    }

    Vector2f bottomRight = new Vector2f(Float.MIN_VALUE, Float.MAX_VALUE);
    Vector2f childMax = new Vector2f();

    Rectangle rectangle = new Rectangle();

    for (Node child : children) {
      RenderElement re = child.getRenderElement();
      re.getBounds(rectangle);

      childMax.x = rectangle.getPosition().x + rectangle.getSize().x;
      childMax.y = rectangle.getPosition().y;

      bottomRight.x = Math.max(childMax.x, bottomRight.x);
      bottomRight.y = Math.min(childMax.y, bottomRight.y);
    }

    Vector2f contentBottomRight = new Vector2f();
    renderElement.getContentEnd(contentBottomRight);

    float difX = Math.max(bottomRight.x - contentBottomRight.x, 0);
    float difY = Math.max(contentBottomRight.y - bottomRight.y, 0);

    renderElement.getContentExtension().set(difX, difY);
    renderElement.spawn();
  }

  public void moveTo(Vector2f pos) {
    Vector2f currentPos = new Vector2f(renderElement.getPosition());
    renderElement.moveTo(getOwning(), pos);

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
      child.moveTo(newChildPos);
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
    if (this.hidden == hidden) {
      return;
    }

    this.hidden = hidden;
    this.renderElement.setHidden(hidden);

    if (hidden) {
      forEachEntity(HideUtil::hide);
    } else {
      forEachEntity(HideUtil::unhide);
    }
  }

  public void setDirection(Align direction) {
    Objects.requireNonNull(direction, "Null direction");
    this.direction = direction;
  }

  public abstract void visitorEnter(Visitor visitor);
  public abstract void visitorExit(Visitor visitor);

  /* --------------------------- Event listeners ---------------------------- */

  public void dispatchEvent(Event event) {
    dispatchOnlySelf(event);
    boolean bubbling = event.isBubbling();

    if (bubbling) {
      Node n = parent;

      while (n != null && !event.propagationStopped()) {
        n.dispatchOnlySelf(event);
        n = n.parent;
      }
    }

    getOwning().dispatchEvent(event);
  }

  private void dispatchOnlySelf(Event event) {
    String type = event.getEventType();

    event.setCurrentTarget(this);

    List<EventListener> listenerList = getListeners(type);

    if (listenerList != null && !listenerList.isEmpty()) {
      EventExecutionTask task = new EventExecutionTask(event, listenerList);
      getOwning().getExecutor().execute(task);
    }
  }

  protected void kill() {
    renderElement.kill();

    for (Node child : children) {
      child.kill();
    }
  }
}
