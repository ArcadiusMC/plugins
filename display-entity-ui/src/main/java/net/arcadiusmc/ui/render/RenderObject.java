package net.arcadiusmc.ui.render;

import java.util.ArrayList;
import java.util.List;
import net.arcadiusmc.ui.style.StyleRuleList;
import org.bukkit.entity.Display;
import org.joml.Vector2f;
import org.w3c.dom.Node;

abstract class RenderObject {

  private static final NodeEntity[] ENTITY_VALUES = NodeEntity.values();

  public static final int SUBLAYER_COUNT = ENTITY_VALUES.length;

  public static final float LAYER_DEPTH = 0.1f;
  public static final float SUBLAYER_DEPTH = LAYER_DEPTH / SUBLAYER_COUNT;

  final Node node;
  final DocumentRender render;

  RenderObject parent;
  final List<RenderObject> children = new ArrayList<>();

  private final Display[] entities;

  final Vector2f min = new Vector2f();
  final Vector2f max = new Vector2f();

  float contentWidth;
  float contentHeight;

  float width;
  float height;

  StyleRuleList styleRules = new StyleRuleList();

  public RenderObject(Node node, DocumentRender render) {
    this.node = node;
    this.render = render;
    this.entities = new Display[SUBLAYER_COUNT];
  }

  public Display getSublayer(NodeEntity entity) {
    return entities[entity.ordinal()];
  }

  public Display setSublayer(NodeEntity entity, Display display) {
    Display existing = getSublayer(entity);
    entities[entity.ordinal()] = display;
    return existing;
  }

  public void reconfigureSize() {
    max.set(min);
    max.add(width, height);
  }

  public void moveTo(float x, float y) {
    min.set(x, y);
    min.add(width, height, max);
  }

  abstract <C, R> R visit(Visitor<C, R> visitor, C context);
}
