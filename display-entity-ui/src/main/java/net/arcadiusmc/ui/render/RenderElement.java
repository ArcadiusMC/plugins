package net.arcadiusmc.ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.math.Screen;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

@Getter
public class RenderElement {
  //
  // Unless stated otherwise, all coordinates are in world space
  //

  private static final Logger LOGGER = Loggers.getLogger();

  /**
   * Scale value of a text display with no text to make it take up 1 block of space
   */
  public static final float EMPTY_TD_BLOCK_SIZE = 40.0f;
  public static final float CHAR_PX_SIZE = 1.0f / EMPTY_TD_BLOCK_SIZE;
  public static final float LINE_HEIGHT = CHAR_PX_SIZE * (8 + 2); // 8 for character, 2 for descender space
  public static final float LAYER_DEPTH = 0.001f;
  public static final float ITEM_PX_TO_CH_PX = 2.5f;
  public static final float ITEM_Z_WIDTH = 0.001f;
  public static final float ITEM_SPRITE_SIZE = ITEM_PX_TO_CH_PX * CHAR_PX_SIZE * 16;
  public static final boolean SEE_THROUGH = false;

  public static final Color NIL_COLOR = Color.fromARGB(0, 0, 0, 0);

  public static final int LAYER_COUNT = RenderLayer.values().length;

  static final Brightness BRIGHTNESS = new Brightness(15, 15);

  private final World world;
  private final Screen screen;

  private final Vector2f position = new Vector2f(0);

  private final Vector2f contentScale = new Vector2f(1);
  private boolean textShadowed;

  private Color backgroundColor = Color.BLACK;
  private Color outlineColor = Color.WHITE;

  // x: left, y: top, z: bottom, w: right
  private final Vector4f paddingSize = new Vector4f(0);
  private final Vector4f outlineSize = new Vector4f(0);

  private final Layer[] layers = new Layer[LAYER_COUNT];

  @Setter
  private ElementContent content;

  private final List<RenderElement> children = new ArrayList<>();
  private RenderElement parent;

  public RenderElement(World world, Screen screen) {
    this.world = world;
    this.screen = screen;
  }

  public void addChild(RenderElement child) {
    child.parent = this;
    children.add(child);
  }

  public final Layer getLayer(RenderLayer layer) {
    Layer l = layers[layer.ordinal()];

    if (l == null) {
      l = new Layer(layer);
      layers[layer.ordinal()] = l;
    }

    return l;
  }

  public boolean isContentEmpty() {
    return content == null || content.isEmpty();
  }

  private static boolean isNotZero(Vector4f v) {
    return v.x > 0 || v.y > 0 || v.z > 0 || v.w > 0;
  }

  public void spawn(PageView view) {
    for (RenderElement child : children) {
      child.spawn(view);
    }

    spawnSelf(view);
  }

  public void spawnSelf(PageView view) {
    Vector3f pos = new Vector3f();
    Vector2f rot = screen.getRotation();

    screen.screenToWorld(position, pos);

    Location location = new Location(world, pos.x, pos.y, pos.z, rot.x, rot.y);
    Layer content = getLayer(RenderLayer.CONTENT);

    // Step 1 - Spawn content
    if (isContentEmpty()) {
      content.size.set(0);

      Display entity = content.entity;
      if (entity != null) {
        view.removeEntity(entity);
      }

      content.killEntity();
    } else {
      Display display = this.content.createEntity(world, location);
      configureDisplay(display);

      if (display instanceof TextDisplay td) {
        td.setShadowed(textShadowed);
      }

      content.setEntity(display);
      view.addEntity(display);

      this.content.measureContent(content.size);
      this.content.configureInitial(content, this);

      content.size.mul(contentScale);
      content.scale.x = contentScale.x;
      content.scale.y = contentScale.y;

      // Early Step 6 - Offset content layer by half it's length
      content.translate.x += (content.size.x / 2.0f);
      content.updateTransform();
    }

    // Step 2 - Spawn background

    // calls to setBackgroundColor and setOutlineColor
    // to force the entities to update
    if (isNotZero(paddingSize)) {
      createLayerEntity(RenderLayer.BACKGROUND, location, view);
      setBackgroundColor(backgroundColor);
    }

    // Step 3 - Spawn outline
    if (isNotZero(outlineSize)) {
      createLayerEntity(RenderLayer.OUTLINE, location, view);
      setOutlineColor(outlineColor);
    }

    // Step 4 - Set layer sizes
    calculateLayerSizes();

    // Step 5 - X and Y layer offsets
    applyBorderOffsets();

    // Step 7 - Apply layer specific screen normal offset
    applyScreenNormalOffsets();

    // Step 8 - Apply screen rotation and offset by height
    applyScreenRotation(rot);

    // Step 9 - Apply transformations to entities
    forEachSpawedLayer(LayerDirection.FORWARD, (layer, iteratedCount) -> {
      layer.updateTransform();
    });
  }

  private void configureDisplay(Display display) {
    display.setBrightness(BRIGHTNESS);

    if (display instanceof TextDisplay td) {
      td.setSeeThrough(SEE_THROUGH);
    }
  }

  private void applyScreenRotation(Vector2f rot) {
    Quaternionf lrot = new Quaternionf();
    lrot.rotateY((float) Math.toRadians(rot.x));
    lrot.rotateX((float) Math.toRadians(rot.y));

    forEachSpawedLayer(LayerDirection.FORWARD, (layer, iteratedCount) -> {
      layer.translate.rotate(lrot);
    });
  }

  private void calculateLayerSizes() {
    forEachSpawedLayer(LayerDirection.FORWARD, (layer, count) -> {
      Vector4f increase = switch (layer.layer) {
        case BACKGROUND -> paddingSize;
        case OUTLINE -> outlineSize;
        default -> new Vector4f();
      };

      if (layer.layer != RenderLayer.CONTENT) {
        layer.size.x += increase.x + increase.w;
        layer.size.y += increase.y + increase.z;
        layer.scale.x = EMPTY_TD_BLOCK_SIZE * layer.size.x;
        layer.scale.y = EMPTY_TD_BLOCK_SIZE * layer.size.y;
      }

      layer.translate.y -= layer.size.y;
      layer.borderSize.set(increase);

      Layer next = nextSpawned(layer);

      if (next == null) {
        return;
      }

      next.size.add(layer.size);
    });
  }

  private int getDepth() {
    int d = 0;
    RenderElement parent = this.parent;

    while (parent != null) {
      d += parent.getSpawnedLayerCount();
      parent = parent.parent;
    }

    return d;
  }

  private int getSpawnedLayerCount() {
    int result = 0;

    if (isNotZero(outlineSize)) {
      result++;
    }
    if (isNotZero(paddingSize)) {
      result++;
    }
    if (!isContentEmpty()) {
      result++;
    }

    return result;
  }

  private void applyScreenNormalOffsets() {
    int depth = getDepth();

    forEachSpawedLayer(LayerDirection.BACKWARD, (layer, iteratedCount) -> {
      layer.translate.z -= ((iteratedCount + depth) * LAYER_DEPTH);
    });
  }

  private void applyBorderOffsets() {
    Vector2f offsetStack = new Vector2f(0);

    forEachSpawedLayer(LayerDirection.BACKWARD, (layer, count) -> {
      Layer next = nextSpawned(layer);

      if (next == null) {
        return;
      }

      layer.translate.x += offsetStack.x += next.borderSize.x;
      layer.translate.y -= offsetStack.y += next.borderSize.z;
    });
  }

  private void createLayerEntity(RenderLayer rLayer, Location location, PageView view) {
    Layer layer = getLayer(rLayer);

    TextDisplay display = world.spawn(location, TextDisplay.class);
    configureDisplay(display);

    layer.setEntity(display);
    view.addEntity(display);
  }

  private Layer nextSpawned(Layer layer) {
    int ord = layer.layer.ordinal();

    while (true) {
      ord++;

      if (ord >= LAYER_COUNT) {
        return null;
      }

      Layer l = layers[ord];

      if (l == null || !l.isSpawned()) {
        continue;
      }

      return l;
    }
  }

  public void forEachSpawedLayer(LayerDirection dir, LayerOp op) {
    int start = dir.start;
    int iterated = 0;

    for (int i = start; i < LAYER_COUNT && i >= 0; i += dir.modifier) {
      Layer layer = layers[i];

      if (layer == null || !layer.isSpawned()) {
        continue;
      }

      op.accept(layer, iterated);
      iterated++;
    }
  }

  private <S extends Display> void applyLayerAs(RenderLayer layer, Class<S> type, Consumer<S> op) {
    applyLayer(layer, display -> {
      if (!type.isInstance(display)) {
        return;
      }

      op.accept((S) display);
    });
  }

  private void applyLayer(RenderLayer layer, Consumer<Display> consumer) {
    Layer l = layers[layer.ordinal()];

    if (l == null || !l.isSpawned()) {
      return;
    }

    consumer.accept(l.entity);
  }

  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;

    applyLayerAs(RenderLayer.BACKGROUND, TextDisplay.class, d -> {
      d.setBackgroundColor(backgroundColor);
    });
  }

  public void setOutlineColor(Color outlineColor) {
    this.outlineColor = outlineColor;

    applyLayerAs(RenderLayer.OUTLINE, TextDisplay.class, d -> {
      d.setBackgroundColor(outlineColor);
    });
  }

  public void setTextShadowed(boolean textShadowed) {
    this.textShadowed = textShadowed;

    applyLayerAs(RenderLayer.OUTLINE, TextDisplay.class, d -> {
      d.setShadowed(textShadowed);
    });
  }

  enum LayerDirection {
    /** Starts from {@link RenderLayer#CONTENT}, moves towards {@link RenderLayer#OUTLINE} */
    FORWARD (0, 1),

    /** Starts from {@link RenderLayer#OUTLINE}, moves towards {@link RenderLayer#CONTENT} */
    BACKWARD (LAYER_COUNT - 1, -1);

    final int start;
    final int modifier;

    LayerDirection(int start, int modifier) {
      this.start = start;
      this.modifier = modifier;
    }
  }

  @Getter @ToString
  public static class Layer {

    final RenderLayer layer;

    final Vector2f size = new Vector2f(0);
    final Vector4f borderSize = new Vector4f(0);

    @Setter
    Display entity;

    final Vector3f scale = new Vector3f(1);
    final Vector3f translate = new Vector3f();
    final Quaternionf leftRotation = new Quaternionf();
    float zScale = 1;

    public Layer(RenderLayer layer) {
      this.layer = layer;
    }

    public boolean isSpawned() {
      return entity != null && !entity.isDead();
    }

    public void killEntity() {
      if (entity == null) {
        return;
      }

      entity.remove();
      entity = null;
    }

    public void updateTransform() {
      if (!isSpawned()) {
        return;
      }

      Transformation trans = entity.getTransformation();
      Vector3f sc = trans.getScale();
      Vector3f tr = trans.getTranslation();

      tr.x = translate.z;
      tr.y = translate.y;
      tr.z = translate.x;

      sc.x = scale.x;
      sc.y = scale.y;
      sc.z = zScale;

      trans.getLeftRotation().set(leftRotation);

      entity.setTransformation(trans);
    }
  }

  public interface LayerOp {
    void accept(Layer layer, int iteratedCount);
  }
}
