package net.arcadiusmc.ui.render;

import static net.arcadiusmc.ui.render.RenderLayer.LAYER_COUNT;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.HideUtil;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.math.Rectangle;
import net.arcadiusmc.ui.struct.AlignDirection;
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

  // Macro layer = A single element
  // Micro layer = A single layer of an element
  public static final float MICRO_LAYER_DEPTH = 0.001f;
  public static final float MACRO_LAYER_DEPTH = MICRO_LAYER_DEPTH * LAYER_COUNT;

  public static final float ITEM_PX_TO_CH_PX = 2.5f;
  public static final float ITEM_Z_WIDTH = 0.001f;
  public static final float ITEM_SPRITE_SIZE = ITEM_PX_TO_CH_PX * CHAR_PX_SIZE * 16;
  public static final boolean SEE_THROUGH = false;

  public static final Color NIL_COLOR = Color.fromARGB(0, 0, 0, 0);

  static final Brightness BRIGHTNESS = new Brightness(15, 15);

  private final Vector2f position = new Vector2f(0);

  private final Vector2f contentScale = new Vector2f(1);
  private boolean textShadowed;

  private Color backgroundColor = Color.BLACK;
  private Color outlineColor = Color.WHITE;

  @Setter
  private boolean hidden = false;

  /*
   * x: left, y: top, z: bottom, w: right
   *
   *   y
   * x + w
   *   z
   */
  private final Vector4f paddingSize = new Vector4f(0);
  private final Vector4f outlineSize = new Vector4f(0);
  private final Vector4f marginSize = new Vector4f(0);

  private final Vector2f contentExtension = new Vector2f();

  /** Depth value multiplier */
  @Setter
  private float depth = 0;

  private final Layer[] layers = new Layer[LAYER_COUNT];

  @Setter
  private ElementContent content;

  private static boolean isNotSpawned(Layer layer) {
    return layer == null || !layer.isSpawned();
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

  public void moveTo(PageView view, Vector2f screenPos) {
    this.position.set(screenPos);
    Location loc = getSpawnLocation(view);

    forEachSpawedLayer(LayerDirection.FORWARD, (layer, iteratedCount) -> {
      layer.entity.teleport(loc);
    });
  }

  public void getContentStart(Vector2f out) {
    float topDif = paddingSize.y + outlineSize.y;
    float leftDif = paddingSize.x + outlineSize.x;

    out.set(position);
    out.add(leftDif, -topDif);
  }

  public void getContentEnd(Vector2f out) {
    getContentStart(out);
    Layer content = getLayer(RenderLayer.CONTENT);
    out.x += content.size.x;
    out.y -= content.size.y;
  }

  public void getAlignmentPosition(Vector2f out, AlignDirection direction) {
    getContentStart(out);
    Layer content = layers[RenderLayer.CONTENT.ordinal()];

    if (isNotSpawned(content)) {
      return;
    }

    if (direction == AlignDirection.X) {
      out.add(content.size.x, 0);
    } else {
      out.sub(0, content.size.y);
    }
  }

  public void getElementSize(Vector2f out) {
    for (int i = LAYER_COUNT - 1; i >= 0; i--) {
      Layer l = layers[i];

      if (isNotSpawned(l)) {
        continue;
      }

      out.set(l.size);
      return;
    }

    out.set(0);
  }

  public void getBounds(Rectangle rectangle) {
    getElementSize(rectangle.getSize());
    rectangle.getPosition().set(position);
  }

  public void getMarginBounds(Rectangle rectangle) {
    getBounds(rectangle);

    rectangle.getPosition().x -= marginSize.x;
    rectangle.getPosition().y += marginSize.y;
    rectangle.getSize().x += marginSize.x + marginSize.w;
    rectangle.getSize().y += marginSize.y + marginSize.z;
  }

  private Location getSpawnLocation(PageView view) {
    Vector3f pos = new Vector3f();
    Vector2f rot = view.getBounds().getRotation();

    view.getBounds().screenToWorld(position, pos);
    return new Location(view.getWorld(), pos.x, pos.y, pos.z, rot.x, rot.y);
  }

  private void killLayerEntity(Layer layer, PageView view) {
    if (layer.entity == null) {
      return;
    }

    view.removeEntity(layer.entity);
    layer.killEntity();
  }

  public void spawn(PageView view) {
    Location location = getSpawnLocation(view);
    Layer content = getLayer(RenderLayer.CONTENT);
    content.zeroValues();

    World world = view.getWorld();

    // Step 1 - Spawn content
    if (isContentEmpty()) {
      killLayerEntity(content, view);
    } else {
      Display display = this.content.createEntity(world, location);
      configureDisplay(display);

      if (display instanceof TextDisplay td) {
        td.setShadowed(textShadowed);
      }

      killLayerEntity(content, view);

      content.setEntity(display);
      view.addEntity(display);

      this.content.measureContent(content.size);
      this.content.configureInitial(content, this);

      content.size.mul(contentScale);
      content.scale.x = contentScale.x;
      content.scale.y = contentScale.y;

      // Early Step 6 - Offset content layer by half it's length
      content.translate.x += (content.size.x / 2.0f);
    }

    // Step 2 - Spawn background
    if (isNotZero(paddingSize)) {
      createLayerEntity(RenderLayer.BACKGROUND, location, view, backgroundColor, paddingSize);
    }

    // Step 3 - Spawn outline
    if (isNotZero(outlineSize)) {
      createLayerEntity(RenderLayer.OUTLINE, location, view, outlineColor, outlineSize);
    }

    // Step 4 - Set layer sizes
    calculateLayerSizes();

    // Step 5 - X and Y layer offsets
    applyBorderOffsets();

    // Step 7 - Apply layer specific screen normal offset
    applyScreenNormalOffsets();

    // Step 8 - Apply screen rotation and offset by height
    applyScreenRotation(location.getYaw(), location.getPitch());

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

    if (hidden) {
      HideUtil.hide(display);
    }
  }

  private void applyScreenRotation(float yaw, float pitch) {
    Quaternionf lrot = new Quaternionf();
    lrot.rotateY((float) Math.toRadians(yaw));
    lrot.rotateX((float) Math.toRadians(pitch));

    forEachSpawedLayer(LayerDirection.FORWARD, (layer, iteratedCount) -> {
      // Add calculated values
      layer.translate.z -= layer.depth;
      layer.translate.x += layer.borderOffset.x;
      layer.translate.y -= layer.borderOffset.y;

      // Perform rotation
      layer.translate.rotate(lrot);
    });
  }

  private void calculateLayerSizes() {
    LayerIterator it = layerIterator(LayerDirection.FORWARD);
    boolean extensionApplied = false;

    while (it.hasNext()) {
      int count = it.getCount();
      Layer layer = it.next();

      if (layer.layer != RenderLayer.CONTENT) {
        Vector4f increase = layer.borderSize;

        layer.size.x += increase.x + increase.w;
        layer.size.y += increase.y + increase.z;

        if (!extensionApplied) {
          layer.size.x += contentExtension.x;
          layer.size.y += contentExtension.y;

          extensionApplied = true;
        }

        layer.scale.x = EMPTY_TD_BLOCK_SIZE * layer.size.x;
        layer.scale.y = EMPTY_TD_BLOCK_SIZE * layer.size.y;
      }

      layer.translate.y -= layer.size.y;
      Layer next = nextSpawned(layer);

      if (next == null) {
        continue;
      }

      next.size.add(layer.size);
    }
  }

  private void applyScreenNormalOffsets() {
    forEachSpawedLayer(LayerDirection.BACKWARD, (layer, iteratedCount) -> {
      float micro = iteratedCount * MICRO_LAYER_DEPTH;
      float macro = this.depth * MACRO_LAYER_DEPTH;

      layer.depth = micro;
      layer.depth += macro;
    });
  }

  private void applyBorderOffsets() {
    Vector2f offsetStack = new Vector2f(0);

    forEachSpawedLayer(LayerDirection.BACKWARD, (layer, count) -> {
      Layer next = nextSpawned(layer);

      if (next == null) {
        return;
      }

      layer.borderOffset.x += offsetStack.x += next.borderSize.x;
      layer.borderOffset.y += offsetStack.y += next.borderSize.z;
    });
  }

  private void createLayerEntity(
      RenderLayer rLayer,
      Location location,
      PageView view,
      Color color,
      Vector4f borderSize
  ) {
    Layer layer = getLayer(rLayer);
    layer.zeroValues();
    layer.borderSize.set(borderSize);
    killLayerEntity(layer, view);

    TextDisplay display = location.getWorld().spawn(location, TextDisplay.class);
    display.setBackgroundColor(color);
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

      if (isNotSpawned(l)) {
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

      if (isNotSpawned(layer)) {
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

    if (isNotSpawned(l)) {
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

  LayerIterator layerIterator(LayerDirection direction) {
    return new LayerIterator(direction.modifier, direction.start);
  }

  class LayerIterator implements Iterator<Layer> {

    private int dir;
    private int index;

    @Getter
    private int count;

    public LayerIterator(int dir, int index) {
      this.dir = dir;
      this.index = index;
    }

    private boolean inBounds(int idx) {
      return idx >= 0 && idx < LAYER_COUNT;
    }

    @Override
    public boolean hasNext() {
      if (!inBounds(index)) {
        return false;
      }

      while (inBounds(index)) {
        Layer layer = layers[index];

        if (isNotSpawned(layer)) {
          index += dir;
          continue;
        }

        return true;
      }

      return false;
    }

    @Override
    public Layer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      Layer l = layers[index];
      index += dir;
      count++;

      return l;
    }
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
    final Vector2f borderOffset = new Vector2f(0);

    float depth;

    @Setter
    Display entity;

    final Vector3f scale = new Vector3f(1);
    final Vector3f translate = new Vector3f();
    final Quaternionf leftRotation = new Quaternionf();

    public Layer(RenderLayer layer) {
      this.layer = layer;
    }

    void zeroValues() {
      size.set(0);
      borderSize.set(0);
      borderOffset.set(0);
      depth = 0;

      scale.set(1);
      translate.set(0);
      leftRotation.set(0, 0, 0, 1);
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
      sc.z = scale.z;

      trans.getLeftRotation().set(leftRotation);

      entity.setTransformation(trans);
    }
  }

  public interface LayerOp {
    void accept(Layer layer, int iteratedCount);
  }
}
