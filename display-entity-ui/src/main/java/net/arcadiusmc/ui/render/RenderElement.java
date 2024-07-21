package net.arcadiusmc.ui.render;

import static net.arcadiusmc.ui.render.RenderLayer.LAYER_COUNT;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.math.Rectangle;
import net.arcadiusmc.ui.struct.Align;
import net.arcadiusmc.ui.math.Rect;
import net.arcadiusmc.ui.style.StylePropertyMap;
import net.arcadiusmc.ui.util.HideUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
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

  /** Global element scale */
  public static final float GLOBAL_SCALAR = .5f;

  // Macro layer = A single element
  // Micro layer = A single layer of an element (eg: content, background, outline)
  public static final float MICRO_LAYER_DEPTH = 0.001f;
  public static final float MACRO_LAYER_DEPTH = MICRO_LAYER_DEPTH * LAYER_COUNT;

  public static final float ITEM_PX_TO_CH_PX = 2.5f;
  public static final float ITEM_Z_WIDTH = 0.001f;
  public static final float ITEM_SPRITE_SIZE = ITEM_PX_TO_CH_PX * CHAR_PX_SIZE * 16;
  public static final boolean SEE_THROUGH = false;

  public static final Color NIL_COLOR = Color.fromARGB(0, 0, 0, 0);

  static final Brightness BRIGHTNESS = new Brightness(15, 15);

  private final Document owning;

  private final Layer[] layers = new Layer[LAYER_COUNT];
  private final StylePropertyMap styleProperties = new StylePropertyMap();

  private final Vector2f position = new Vector2f(0);
  private final Vector2f contentScale = new Vector2f(1);
  private final Vector2f contentExtension = new Vector2f();
  private final Vector2f minSize = new Vector2f(0);
  private final Vector2f maxSize = new Vector2f(Float.MAX_VALUE);

  private boolean textShadowed;

  private Color backgroundColor = Color.BLACK;
  private Color outlineColor = Color.WHITE;
  private Color borderColor = Color.WHITE;

  private final Rect paddingSize = new Rect();
  private final Rect outlineSize = new Rect();
  private final Rect borderSize = new Rect();

  @Setter
  private float depth = 0;

  @Setter
  private float zOffset;

  private ElementContent content;

  @Setter
  private boolean contentDirty = false;
  private boolean spawned = false;

  @Setter
  private boolean hidden = false;

  public RenderElement(Document owning) {
    this.owning = owning;
  }

  public static boolean isNotSpawned(Layer layer) {
    return layer == null || !layer.isSpawned();
  }

  public boolean hasSpawnedLayer(RenderLayer layer) {
    return !isNotSpawned(layers[layer.ordinal()]);
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

  private static boolean isNotZero(Rect v) {
    return v.left > 0 || v.bottom > 0 || v.top > 0 || v.right > 0;
  }

  public void moveTo(Document view, Vector2f screenPos) {
    this.position.set(screenPos);
    Location loc = getSpawnLocation(view);

    forEachSpawedLayer(LayerDirection.FORWARD, (layer, iteratedCount) -> {
      layer.entity.teleport(loc);
    });
  }

  public void getContentStart(Vector2f out) {
    float topDif = paddingSize.top + outlineSize.top + borderSize.top;
    float leftDif = paddingSize.left + outlineSize.left + borderSize.left;

    out.set(position);
    out.add(leftDif * GLOBAL_SCALAR, -topDif * GLOBAL_SCALAR);
  }

  private Vector2f getContentSize() {
    if (content == null) {
      return new Vector2f();
    }

    Vector2f size = new Vector2f();
    content.measureContent(size, styleProperties);

    size.mul(GLOBAL_SCALAR).mul(contentScale);
    return size;
  }

  public void getContentEnd(Vector2f out) {
    getContentStart(out);

    if (content == null) {
      return;
    }

    out.add(getContentSize());
  }

  public void getAlignmentPosition(Vector2f out, Align direction) {
    getContentStart(out);

    if (content == null) {
      return;
    }

    Vector2f size = getContentSize();

    if (direction == Align.X) {
      out.x += size.x;
    } else {
      out.y -= size.y;
    }
  }

  public void getElementSize(Vector2f out) {
    //
    // let raw_size = (content_size * content_scale) + content_extension
    // let capped_size = clamp(raw_size, min_size, max_size)
    // let result = capped_size + padding_size + outline_size
    //              =========================================
    //

    if (content != null) {
      content.measureContent(out, styleProperties);
    } else {
      out.set(0);
    }

    out.mul(GLOBAL_SCALAR);
    out.mul(contentScale);
    out.add(contentExtension);
    out.max(minSize).min(maxSize);

    float xAdd
        = paddingSize.left + paddingSize.right
        + outlineSize.left + outlineSize.right
        + borderSize.left + borderSize.right;

    float yAdd
        = paddingSize.top + paddingSize.bottom
        + outlineSize.top + outlineSize.bottom
        + borderSize.top + borderSize.bottom;

    out.x += xAdd * GLOBAL_SCALAR;
    out.y += yAdd * GLOBAL_SCALAR;
  }

  public void getBounds(Rectangle rectangle) {
    Vector2f pos = rectangle.getPosition();
    Vector2f size = rectangle.getSize();

    getElementSize(size);

    pos.x = position.x;
    pos.y = position.y - size.y;
  }

  private Location getSpawnLocation(Document view) {
    Vector3f pos = new Vector3f();
    Vector2f rot = view.getScreen().getRotation();

    view.getScreen().screenToWorld(position, pos);
    return new Location(view.getWorld(), pos.x, pos.y, pos.z, rot.x, rot.y);
  }

  /* --------------------------- Spawning process ---------------------------- */

  public void update() {
    if (!spawned) {
      return;
    }

    spawn();

    /*
     * I remembered I don't care, just call spawn again
     *

    ElementChanges set = new ElementChanges(changes);

    if (set.has(CONTENT)) {
      spawn(view);
      return;
    }

    if (set.hasAny(CONTENT_EXT | PADDING)) {
      // Update all layers from padding onwards
      // change their scale
      // that's abt it
    } else if (set.has(OUTLINE_SIZE)) {
      // Just change the last layer
    }

    if (set.has(OUTLINE_COLOR)) {
      // Update entity color
    }

    if (set.has(BG_COLOR)) {
      // Update entity coolor
    }*/
  }

  public void spawn() {
    Location location = getSpawnLocation(owning);
    Layer content = getLayer(RenderLayer.CONTENT);
    content.zeroValues();

    // Step 1 - Spawn content
    if (isContentEmpty()) {
      killLayerEntity(content);
    } else {
      Display display = getOrCreateContentEntity(content, location);

      if (display instanceof TextDisplay td) {
        td.setShadowed(textShadowed);
      }

      content.size.mul(GLOBAL_SCALAR);
      content.size.mul(contentScale);

      content.scale.x = GLOBAL_SCALAR;
      content.scale.y = GLOBAL_SCALAR;
      content.scale.x *= contentScale.x;
      content.scale.y *= contentScale.y;

      // Early Step 6 - Offset content layer by half it's length
      content.translate.x += (content.size.x * 0.5f);
    }

    // Step 2 - Spawn background
    createLayerEntity(RenderLayer.BACKGROUND, location, backgroundColor, paddingSize);

    if (isNotZero(borderSize)) {
      createLayerEntity(RenderLayer.BORDER, location, borderColor, borderSize);
    }
    // Step 3 - Spawn outline
    if (isNotZero(outlineSize)) {
      createLayerEntity(RenderLayer.OUTLINE, location, outlineColor, outlineSize);
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

    this.spawned = true;
  }

  private Display getOrCreateContentEntity(Layer content, Location location) {
    boolean requiresRespawn;
    ElementContent ec = this.content;

    if (!content.isSpawned()) {
      requiresRespawn = true;
    } else if (ec != null && !ec.getEntityClass().isInstance(content.entity)) {
      requiresRespawn = true;
    } else {
      requiresRespawn = false;
    }

    Display display;

    if (requiresRespawn) {
      killLayerEntity(content);

      display = ec.createEntity(location.getWorld(), location);
      ec.applyContentTo(display, styleProperties);

      content.setEntity(display);
      owning.addEntity(display);
    } else {
      display = content.entity;

      if (contentDirty && ec != null) {
        ec.applyContentTo(display, styleProperties);
      }
    }

    if (ec != null) {
      ec.measureContent(content.size, styleProperties);
      ec.configureInitial(content, this);
    }

    configureDisplay(display);

    return display;
  }

  private void killLayerEntity(Layer layer) {
    if (layer.entity == null) {
      return;
    }

    owning.removeEntity(layer.entity);
    layer.killEntity();
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

      // Perform rotation
      layer.translate.rotate(lrot, layer.rotatedTranslate);
    });
  }

  private void calculateLayerSizes() {
    LayerIterator it = layerIterator(LayerDirection.FORWARD);
    boolean extensionApplied = false;

    while (it.hasNext()) {
      Layer layer = it.next();

      if (layer.layer != RenderLayer.CONTENT) {
        Rect increase = layer.borderSize;

        layer.size.x += increase.left + increase.right;
        layer.size.y += increase.top + increase.bottom;

        if (!extensionApplied) {
          layer.size.x += contentExtension.x;
          layer.size.y += contentExtension.y;

          layer.size.max(minSize);
          layer.size.min(maxSize);

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
      layer.depth += zOffset;
    });
  }

  private void applyBorderOffsets() {
    Vector2f offsetStack = new Vector2f(0);

    forEachSpawedLayer(LayerDirection.BACKWARD, (layer, count) -> {
      Layer next = nextSpawned(layer);

      if (next == null) {
        return;
      }

      layer.translate.x += offsetStack.x += next.borderSize.left;
      layer.translate.y -= offsetStack.y += next.borderSize.bottom;
    });
  }

  /* --------------------------- Setters ---------------------------- */

  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;

    applyLayerAs(RenderLayer.BACKGROUND, TextDisplay.class, d -> {
      d.setBackgroundColor(backgroundColor);
    });
  }

  public void setOutlineColor(Color outlineColor) {
    this.outlineColor = outlineColor;
    applyLayerAs(RenderLayer.OUTLINE, TextDisplay.class, d -> d.setBackgroundColor(outlineColor));
  }

  public void setBorderColor(Color color) {
    this.borderColor = color;
    applyLayerAs(RenderLayer.BORDER, TextDisplay.class, d -> d.setBackgroundColor(color));
  }

  public void setTextShadowed(boolean textShadowed) {
    this.textShadowed = textShadowed;

    applyLayerAs(RenderLayer.CONTENT, TextDisplay.class, d -> {
      d.setShadowed(textShadowed);
    });
  }

  public void setContent(ElementContent content) {
    this.content = content;
    this.contentDirty = true;
  }

  /* --------------------------- Layer management ---------------------------- */

  private void createLayerEntity(
      RenderLayer rLayer,
      Location location,
      Color color,
      Rect borderSize
  ) {
    Layer layer = getLayer(rLayer);
    layer.zeroValues();
    layer.borderSize.set(borderSize);
    layer.borderSize.mul(GLOBAL_SCALAR);

    TextDisplay display;

    if (layer.entity != null && !layer.entity.isDead()) {
      display = (TextDisplay) layer.entity;
    } else {
      killLayerEntity(layer);

      display = location.getWorld().spawn(location, TextDisplay.class);

      layer.setEntity(display);
      owning.addEntity(display);
    }

    display.setBackgroundColor(color);
    configureDisplay(display);
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

  LayerIterator layerIterator(LayerDirection direction) {
    return new LayerIterator(direction.modifier, direction.start);
  }

  public void kill() {
    for (Layer layer : layers) {
      if (isNotSpawned(layer)) {
        continue;
      }

      owning.removeEntity(layer.entity);
      layer.killEntity();
    }

    spawned = false;
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
    final Rect borderSize = new Rect();

    float depth;

    @Setter
    Display entity;

    final Vector3f scale = new Vector3f(1);
    final Vector3f translate = new Vector3f(0);
    final Vector3f rotatedTranslate = new Vector3f(0);
    final Quaternionf leftRotation = new Quaternionf();

    public Layer(RenderLayer layer) {
      this.layer = layer;
    }

    void zeroValues() {
      size.set(0);
      borderSize.set(0);
      depth = 0;

      scale.set(1);
      translate.set(0);
      rotatedTranslate.set(0);
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

      // I assure you, dear maintainer, the axes on this translate
      // being screwed are completely vital to this system's
      // continued functioning.
      //
      // Honestly, though, I don't know why this is like this, but it
      // ensures that the Z axes acts like a depth value while X and Y
      // are screen translation values.
      tr.x = rotatedTranslate.z;
      tr.y = rotatedTranslate.y;
      tr.z = rotatedTranslate.x;

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
