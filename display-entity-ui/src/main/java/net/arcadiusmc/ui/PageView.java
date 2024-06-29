package net.arcadiusmc.ui;

import static net.arcadiusmc.ui.render.RenderElement.CHAR_PX_SIZE;
import static net.arcadiusmc.ui.render.RenderElement.EMPTY_TD_BLOCK_SIZE;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.render.TextContent;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.VanillaAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;

@Getter @Setter
public class PageView {

  private static final Logger LOGGER = Loggers.getLogger();

  private final ObjectSet<Display> entities = new ObjectOpenHashSet<>();

  private Screen bounds;
  private World world;
  private Player player;
  private PlayerSession session;

  boolean selected = false;

  private Vector2f cursorPos = new Vector2f(-1);

  private RenderElement bg;
  private RenderElement tooltip;

  public PageView(World world) {
    Objects.requireNonNull(world, "Null world");
    this.world = world;
  }

  public void onInteract(PageInteraction interaction) {
    selected = true;
    Vector3f hitPos = interaction.worldPos();

    Particle.DUST.builder()
        .color(Color.GREEN, 5f)
        .location(world, hitPos.x, hitPos.y, hitPos.z)
        .receivers(player)
        .spawn();
  }

  public void onSelect(Vector2f screenPoint, Vector3f worldPoint) {
    selected = true;
    cursorPos.set(screenPoint);

    bg.setBackgroundColor(Color.GRAY.mixColors(Color.WHITE));

//    for (Display entity : entities) {
//      if ((!(entity instanceof TextDisplay td)) || !td.getScoreboardTags().contains("bg")) {
//        continue;
//      }
//
//      td.setBackgroundColor(Color.GRAY.mixColors(Color.WHITE));
//    }

    Plugin plugin = PluginUtil.getPlugin();
    findHoverText(display -> {
      player.showEntity(plugin, display);
    });
  }

  public void cursorMoveTo(Vector2f screenPoint, Vector3f worldPoint) {
    if (tooltip != null) {
      tooltip.getPosition().set(screenPoint);
    }

    findHoverText(display -> {
      Vector2f rot = bounds.getRotation();

      Vector3f spawnPos = bounds.normal();
      spawnPos.mul(CHAR_PX_SIZE);
      spawnPos.add(worldPoint);

      Location location = new Location(
          world,
          spawnPos.x, spawnPos.y, spawnPos.z,
          rot.x, rot.y
      );

      //LOGGER.debug("hover.loc={}, offset={}", spawnPos, withTrans);

      display.teleport(location);
    });

    cursorPos.set(screenPoint);
  }

  public void onUnselect() {
    selected = false;
    cursorPos.set(-1);

    bg.setBackgroundColor(Color.GRAY);

//    for (Display entity : entities) {
//      if ((!(entity instanceof TextDisplay td)) || !td.getScoreboardTags().contains("bg")) {
//        continue;
//      }
//
//      td.setBackgroundColor(Color.GRAY);
//    }

    Plugin plugin = PluginUtil.getPlugin();
    findHoverText(display -> {
      player.hideEntity(plugin, display);
    });
  }

  private void findHoverText(Consumer<Display> operation) {
    if (true) {
      if (tooltip == null) {
        return;
      }

      for (Layer layer : tooltip.getLayers()) {
        if (layer == null || !layer.isSpawned()) {
          continue;
        }

        operation.accept(layer.getEntity());
      }

      return;
    }

    for (Display entity : entities) {
      if (!entity.getScoreboardTags().contains("mouse_attached")) {
        continue;
      }

      operation.accept((TextDisplay) entity);
    }
  }

  public void addEntity(Display display) {
    display.setPersistent(false);
    entities.add(display);
  }

  public void createBlank() {
    if (world == null) {
      return;
    }

    Vector2f dimensions = bounds.getDimensions();
    dimensions.mul(EMPTY_TD_BLOCK_SIZE);

    TextDisplay display = createEmptyTextDisplay();
    display.addScoreboardTag("bg");

    Transformation transform = display.getTransformation();
    transform.getScale().set(dimensions.x, dimensions.y, 1);
    display.setTransformation(transform);

    addEntity(display);
  }

  public void createTestTooltip() {
    if (world == null) {
      return;
    }

    TextDisplay bg = createEmptyTextDisplay();
    TextDisplay outline = createEmptyTextDisplay();
    TextDisplay text = createEmptyTextDisplay();

    Component content = Component.text("Hello, I'm hover text!", NamedTextColor.YELLOW);
    int lengthPx = TextInfo.length(content);

    bg.setBackgroundColor(Color.BLACK);
    outline.setBackgroundColor(Color.GREEN);
    text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    text.text(content);
    text.setAlignment(TextAlignment.LEFT);

    float textWidth = lengthPx * 1.1f;
    float textHeight = 9;
    float grow = 2.0f;
    float padX = textWidth + grow;
    float padY = textHeight + grow + 1;
    float outlineX = padX + grow;
    float outlineY = padY + grow;

    Transformation textTrans = text.getTransformation();
    Transformation bgTrans = bg.getTransformation();
    Transformation outlineTrans = outline.getTransformation();

    // Do some shit to ensure each element is centered to the next one
    bgTrans.getScale().set(padX, padY, 1);
    outlineTrans.getScale().set(outlineX, outlineY, 1);
    bgTrans.getTranslation().set(0, CHAR_PX_SIZE, -CHAR_PX_SIZE);
    textTrans.getTranslation().set(CHAR_PX_SIZE, -CHAR_PX_SIZE, 0)
        .mul(2)
        .add(0, CHAR_PX_SIZE * 2, -((textWidth / 2) * CHAR_PX_SIZE));

    Vector3f normal = bounds.normal();
    normal.mul(0.001f);

    // Move each layer out a bit to prevent Z fighting
    outlineTrans.getTranslation().add(normal);
    normal.mul(2f);
    bgTrans.getTranslation().add(normal);
    normal.mul(1.5f);
    textTrans.getTranslation().add(normal);

    // Rotate translations
    Quaternionf lrot = new Quaternionf();
    Vector2f rot = bounds.getRotation();
    lrot.rotateY((float) Math.toRadians(rot.x));
    lrot.rotateX((float) Math.toRadians(rot.y));
    outlineTrans.getTranslation().rotate(lrot);
    bgTrans.getTranslation().rotate(lrot);
    textTrans.getTranslation().rotate(lrot);

    // Scale down
    final float scale = 0.5f;
    outlineTrans.getScale().mul(scale);
    bgTrans.getScale().mul(scale);
    textTrans.getScale().mul(scale);
    outlineTrans.getTranslation().mul(scale);
    bgTrans.getTranslation().mul(scale);
    textTrans.getTranslation().mul(scale);

    text.setTransformation(textTrans);
    bg.setTransformation(bgTrans);
    outline.setTransformation(outlineTrans);

    // Attach to mouse
    outline.addScoreboardTag("mouse_attached");
    bg.addScoreboardTag("mouse_attached");
    text.addScoreboardTag("mouse_attached");

    addEntity(bg);
    addEntity(outline);
    addEntity(text);
  }

  public final TextDisplay createEmptyTextDisplay() {
    Location location = bounds.getLowerRightLocation(world);

    TextDisplay display = world.spawn(location, TextDisplay.class);
    //display.setBackgroundColor(Color.WHITE);

    return display;
  }

  public void kill() {
    for (Display entity : entities) {
      entity.remove();
    }

    world = null;
  }

  private void pivotedTransform(Matrix4f mat, Vector3f v) {
    Vector3f center = bounds.center();
    v.sub(center).mulPosition(mat).add(center);
  }

  public void transform(Transformation transformation) {
    Matrix4f matrix = new Matrix4f();
    matrix.translate(transformation.getTranslation());
    matrix.rotate(transformation.getLeftRotation());
    matrix.scale(transformation.getScale());
    matrix.rotate(transformation.getRightRotation());

    bounds.apply(matrix);

    Vector2f rot = bounds.getRotation();
    float yaw = rot.x;
    float pitch = rot.y;

    Vector3f pos = new Vector3f();
    Location location = new Location(world, 0, 0, 0);

    for (Display entity : entities) {
      Transformation oldTrans = entity.getTransformation();

      oldTrans.getScale().mul(transformation.getScale());
      oldTrans.getTranslation().add(transformation.getTranslation());

      entity.setTransformation(oldTrans);

      VanillaAccess.getPositionf(pos, entity);
      pivotedTransform(matrix, pos);

      location.setX(pos.x);
      location.setY(pos.y);
      location.setZ(pos.z);
      location.setYaw(yaw);
      location.setPitch(pitch);

      entity.teleport(location);
    }
  }

  public void removeEntity(Display entity) {
    entities.remove(entity);
  }

  public void spawnBlank() {
    bg = new RenderElement(world, bounds);
    tooltip = new RenderElement(world, bounds);

    Vector2f dim = bounds.getDimensions();

    bg.getPosition().set(dim);
    bg.getPaddingSize().set(0, 0, dim.y, dim.x);
    bg.setBackgroundColor(Color.GRAY);

    RenderElement testChild = new RenderElement(world, bounds);
    testChild.setContent(new TextContent(Component.text("Hello, world!")));
    testChild.getOutlineSize().set(CHAR_PX_SIZE);
    testChild.getPaddingSize().set(CHAR_PX_SIZE);
    testChild.getPosition().set(dim.x - (CHAR_PX_SIZE * 2), dim.y - (CHAR_PX_SIZE * 2));

    bg.addChild(testChild);


    tooltip.setOutlineColor(Color.GREEN);
    tooltip.setBackgroundColor(Color.BLACK);
    tooltip.setContent(new TextContent(Component.text("Hello, I'm a tooltip\n2nd Line", NamedTextColor.YELLOW)));
//    tooltip.setContent(
//        new ItemContent(
//            ItemStacks.builder(Material.TRIDENT)
//                .build()
//        )
//    );
    tooltip.getOutlineSize().set(CHAR_PX_SIZE, CHAR_PX_SIZE, CHAR_PX_SIZE, CHAR_PX_SIZE);
    tooltip.getPaddingSize().set(CHAR_PX_SIZE * 2);
    tooltip.getContentScale().set(0.5f);
    tooltip.setTextShadowed(true);

    LOGGER.debug("------ background.spawn ------");
    bg.spawn(this);

    LOGGER.debug("------ tooltip.spawn ------");
    tooltip.spawn(this);
  }
}
