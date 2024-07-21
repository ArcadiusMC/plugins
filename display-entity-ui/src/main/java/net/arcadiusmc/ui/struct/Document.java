package net.arcadiusmc.ui.struct;

import static net.arcadiusmc.ui.event.Event.FLAG_BUBBLING;
import static net.arcadiusmc.ui.event.Event.FLAG_CANCELLABLE;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.ImmediateExecutor;
import net.arcadiusmc.ui.InteractionType;
import net.arcadiusmc.ui.PageInteraction;
import net.arcadiusmc.ui.PlayerSession;
import net.arcadiusmc.ui.ScrollDirection;
import net.arcadiusmc.ui.event.Event;
import net.arcadiusmc.ui.event.EventExecutionTask;
import net.arcadiusmc.ui.event.EventListener;
import net.arcadiusmc.ui.event.EventListeners;
import net.arcadiusmc.ui.event.EventTarget;
import net.arcadiusmc.ui.event.EventTypes;
import net.arcadiusmc.ui.event.MouseEvent;
import net.arcadiusmc.ui.event.MouseEvent.MouseButton;
import net.arcadiusmc.ui.event.MutationEvent;
import net.arcadiusmc.ui.event.MutationEvent.Action;
import net.arcadiusmc.ui.math.Rectangle;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.style.DocumentStyles;
import net.arcadiusmc.utils.Particles;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3d;

@Getter @Setter
public class Document implements EventTarget {

  private static final Sound CLICK_SOUND = Sound.sound()
      .type(org.bukkit.Sound.UI_BUTTON_CLICK)
      .build();

  /** Amount of ticks a node will remain active or 'clicked' for */
  private static final int CLICK_TICKS = 4;
  public static boolean DEBUG_OUTLINES = false;

  private static final Logger LOGGER = Loggers.getLogger();

  private final ObjectSet<Display> entities = new ObjectOpenHashSet<>();

  private final Screen screen;
  private World world;
  private Player player;
  private PlayerSession session;

  boolean selected = false;

  private final Vector2f cursorPos = new Vector2f(-1);
  private final Vector3f cursorWorldPos = new Vector3f(-1);

  private Element body;

  private Node hoveredNode = null;
  private Node clickedNode = null;
  private MouseButton clickedButton = MouseButton.NONE;
  private int clickedNodeTicks = 0;

  private final Map<String, Element> idLookup = new HashMap<>();
  private final Map<String, String> options = new HashMap<>();
  private final DocumentStyles styles;

  // Runs for all events, even ones which do not bubble
  private final EventListeners listeners = new EventListeners();

  public Document(World world) {
    Objects.requireNonNull(world, "Null world");

    this.screen = new Screen();
    this.world = world;

    this.styles = new DocumentStyles(this);
    this.styles.init();

    addEventListener(EventTypes.MODIFY_ATTR, new IdListener());

    HoverNodeListener hoverListener = new HoverNodeListener();
    addEventListener(EventTypes.MOUSE_ENTER, hoverListener);
    addEventListener(EventTypes.MOUSE_LEAVE, hoverListener);
    addEventListener(EventTypes.MOUSE_MOVE, hoverListener);
  }

  public boolean spawn() {
    if (body == null) {
      return false;
    }

    RenderElement re = body.getRenderElement();
    re.getPosition().y = screen.getHeight();

    styles.recursivelyApplyStyles(body, null);
    body.spawn();
    body.align();

    return true;
  }

  public void dispatchEvent(Event event) {
    if (event.isCancelled()) {
      return;
    }

    event.setCurrentTarget(this);

    List<EventListener> listenerList = this.listeners.getListeners(event.getEventType());

    if (listenerList == null || listenerList.isEmpty()) {
      return;
    }

    EventExecutionTask task = new EventExecutionTask(event, listenerList);
    getExecutor().execute(task);
  }

  public Executor getExecutor() {
    return ImmediateExecutor.EXECUTOR;
  }

  public boolean onScroll(ScrollDirection direction) {
    return false;
  }

  public void onInteract(PageInteraction interaction) {
    selected = true;
    triggerClickEvent(interaction.type());
  }

  public void onSelect(Vector2f screenPoint, Vector3f worldPoint) {
    selected = true;
    cursorPos.set(screenPoint);
    cursorWorldPos.set(worldPoint);
  }

  public void cursorMoveTo(Vector2f screenPoint, Vector3f worldPoint) {
    cursorPos.set(screenPoint);
    cursorWorldPos.set(worldPoint);

    updateSelectedNode();
  }

  public void onUnselect() {
    selected = false;

    cursorPos.set(-1);
    cursorWorldPos.set(-1);

    unselectHovered();
  }

  public void onTick() {
    drawSelected();

    if (clickedNodeTicks <= 0) {
      return;
    }

    clickedNodeTicks--;

    if (clickedNodeTicks > 0) {
      return;
    }

    unselectClickedNode();
  }

  private void drawSelected() {
    if (hoveredNode == null || !DEBUG_OUTLINES) {
      return;
    }

    Rectangle rectangle = new Rectangle();
    hoveredNode.getRenderElement().getBounds(rectangle);

    Vector2f min = rectangle.getPosition();
    Vector2f max = new Vector2f();

    rectangle.getMax(max);

    Vector3f lowerLeft = new Vector3f();
    Vector3f upperRight = new Vector3f();
    Vector3f lowerRight = new Vector3f();
    Vector3f upperLeft = new Vector3f();

    screen.screenToWorld(min, lowerLeft);
    screen.screenToWorld(max, upperRight);

    lowerRight.set(upperRight);
    lowerRight.y = lowerLeft.y;

    upperLeft.set(lowerLeft);
    upperLeft.y = upperRight.y;

    ParticleBuilder builder = Particle.DUST.builder()
        .color(Color.RED, 0.5f)
        .location(world, upperRight.x, upperRight.y, upperRight.z)
        .allPlayers();

    particleLine(lowerLeft, upperLeft, builder);
    particleLine(lowerLeft, lowerRight, builder);
    particleLine(upperLeft, upperRight, builder);
    particleLine(lowerRight, upperRight, builder);
  }

  private void particleLine(Vector3f from, Vector3f to, ParticleBuilder builder) {
    Vector3d spongeFrom = Vector3d.from(from.x, from.y, from.z);
    Vector3d spongeTo = Vector3d.from(to.x, to.y, to.z);
    Particles.line(spongeFrom, spongeTo, 0.12d, world, builder);
  }

  public void addEntity(Display display) {
    display.setPersistent(false);
    entities.add(display);
  }

  public void kill() {
    for (Display entity : entities) {
      entity.remove();
    }

    world = null;
  }

  private void pivotedTransform(Matrix4f mat, Vector3f v) {
    Vector3f center = screen.center();
    v.sub(center).mulPosition(mat).add(center);
  }

  public void transform(Transformation transformation) {
    Matrix4f matrix = new Matrix4f();
    matrix.translate(transformation.getTranslation());
    matrix.rotate(transformation.getLeftRotation());
    matrix.scale(transformation.getScale());
    matrix.rotate(transformation.getRightRotation());

    screen.apply(matrix);

    if (body == null) {
      return;
    }

    RenderElement re = body.getRenderElement();
    re.getPosition().set(0, screen.getHeight());

    if (!body.getRenderElement().isSpawned()) {
      return;
    }

    body.kill();

    styles.recursivelyApplyStyles(body, null);
    body.spawn();
    body.align();

//
//    Vector2f rot = bounds.getRotation();
//    float yaw = rot.x;
//    float pitch = rot.y;
//
//    Vector3f pos = new Vector3f();
//    Location location = new Location(world, 0, 0, 0);
//
//    for (Display entity : entities) {
//      Transformation oldTrans = entity.getTransformation();
//
//      oldTrans.getScale().mul(transformation.getScale());
//
//      entity.setTransformation(oldTrans);
//
//      VanillaAccess.getPositionf(pos, entity);
//      pivotedTransform(matrix, pos);
//
//      location.setX(pos.x);
//      location.setY(pos.y);
//      location.setZ(pos.z);
//      location.setYaw(yaw);
//      location.setPitch(pitch);
//
//      entity.teleport(location);
//    }
  }

  public void removeEntity(Display entity) {
    entities.remove(entity);
  }

  public Element createElement(String tag) {
    Objects.requireNonNull(tag, "Null tag");

    return switch (tag) {
      case Elements.BUTTON -> new ButtonElement(this);
      case Elements.ITEM -> new ItemElement(this);
      case Elements.BODY -> new BodyElement(this);
      default -> new Element(this, tag);
    };
  }

  public TextNode createText() {
    return new TextNode(this);
  }

  /* --------------------------- Selection and input ---------------------------- */

  private MouseEvent fireMouseEvent(
      String type,
      boolean shift,
      MouseButton button,
      Node target,
      int flags
  ) {
    MouseEvent event = new MouseEvent(
        type,
        shift,
        new Vector2f(cursorPos),
        new Vector3f(cursorWorldPos),
        button,
        target,
        flags,
        this
    );

    target.dispatchEvent(event);

    return event;
  }

  private void triggerClickEvent(InteractionType type) {
    if (hoveredNode == null) {
      return;
    }

    if (clickedNode != null && !Objects.equals(clickedNode, hoveredNode)) {
      unselectClickedNode();
    }

    MouseButton button = MouseButton.NONE;
    boolean shift = false;

    switch (type) {
      case SHIFT_LEFT:
        shift = true;
      case LEFT:
        button = MouseButton.LEFT;
        break;

      case SHIFT_RIGHT:
        shift = true;
      case RIGHT:
        button = MouseButton.RIGHT;
        break;
    }

    this.clickedButton = button;
    this.clickedNodeTicks = CLICK_TICKS;
    this.clickedNode = hoveredNode;

    hoveredNode.addFlag(NodeFlag.CLICKED);

    MouseEvent event = fireMouseEvent(
        EventTypes.MOUSE_DOWN,
        shift,
        button,
        clickedNode,
        FLAG_BUBBLING | FLAG_CANCELLABLE
    );

    if (event.isCancelled()) {
      return;
    }

    if (clickedNode instanceof Element e && e.getTagName().equals("button")) {
      player.playSound(CLICK_SOUND);
    }
  }

  private void updateSelectedNode() {
    Node contained = findContainingNode();

    if (contained == null) {
      if (this.hoveredNode == null) {
        return;
      }

      unselectHovered();
      return;
    }

    if (Objects.equals(contained, hoveredNode)) {
      fireMouseEvent(EventTypes.MOUSE_MOVE, false, MouseButton.NONE, this.hoveredNode, 0);
      return;
    }

    unselectHovered();

    this.hoveredNode = contained;
    contained.addFlag(NodeFlag.HOVERED);

    fireMouseEvent(EventTypes.MOUSE_ENTER, false, MouseButton.NONE, contained, 0);
  }

  private void unselectClickedNode() {
    if (clickedNode == null) {
      return;
    }

    clickedNode.removeFlag(NodeFlag.CLICKED);

    fireMouseEvent(EventTypes.CLICK_EXPIRE, false, clickedButton, clickedNode, 0);

    clickedNode = null;
    clickedNodeTicks = 0;
    clickedButton = MouseButton.NONE;
  }

  private void unselectHovered() {
    if (this.hoveredNode == null) {
      return;
    }

    this.hoveredNode.removeFlag(NodeFlag.HOVERED);
    fireMouseEvent(EventTypes.MOUSE_LEAVE, false, MouseButton.NONE, this.hoveredNode, 0);
    this.hoveredNode = null;
  }

  private Node findContainingNode() {
    Node p = body;
    Rectangle rectangle = new Rectangle();

    outer: while (true) {
      if (p.getChildren().isEmpty()) {
        return p;
      }

      for (Node child : p.getChildren()) {
        if (child.ignoredByMouse()) {
          continue;
        }

        child.getRenderElement().getBounds(rectangle);

        if (!rectangle.contains(cursorPos)) {
          continue;
        }

        p = child;
        continue outer;
      }

      return p;
    }
  }

  class IdListener implements EventListener.Typed<MutationEvent> {

    @Override
    public void onEventFired(MutationEvent event) {
      if (event.getAction() != Action.REMOVE_ATTR && event.getAction() != Action.SET_ATTR) {
        return;
      }
      if (!Objects.equals(event.getAttrName(), Attr.ID)) {
        return;
      }

      String previousId = event.getPrevValue();
      String newId = event.getNewValue();
      Element element = (Element) event.getTarget();

      if (!Strings.isNullOrEmpty(previousId)) {
        Element referenced = idLookup.get(previousId);

        if (Objects.equals(referenced, element)) {
          idLookup.remove(previousId);
        }
      }

      if (Strings.isNullOrEmpty(newId)) {
        return;
      }

      idLookup.put(newId, element);
    }
  }

  class HoverNodeListener implements EventListener.Typed<MouseEvent> {

    @Override
    public void onEventFired(MouseEvent event) {
      if (!(event.getTarget() instanceof Element el)) {
        return;
      }

      Node tooltip = el.getTooltip();

      if (tooltip == null) {
        return;
      }

      switch (event.getEventType()) {
        case EventTypes.MOUSE_ENTER -> {
          tooltip.setHidden(false);
          styles.recursivelyApplyStyles(tooltip, null);
          tooltip.spawn();
          tooltip.align();
        }

        case EventTypes.MOUSE_LEAVE -> {
          tooltip.setHidden(true);
        }

        case EventTypes.MOUSE_MOVE -> {
          tooltip.moveTo(event.getScreenPosition());
        }
      }
    }
  }
}
