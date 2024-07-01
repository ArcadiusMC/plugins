package net.arcadiusmc.ui;

import static net.arcadiusmc.ui.render.RenderElement.CHAR_PX_SIZE;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.render.ItemContent;
import net.arcadiusmc.ui.render.RenderElement;
import net.arcadiusmc.ui.render.TextContent;
import net.arcadiusmc.ui.struct.Node;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.VanillaAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
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

  private Node root;
  private Node tooltip;

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

    Plugin plugin = PluginUtil.getPlugin();
    findHoverText(display -> {
      player.showEntity(plugin, display);
    });
  }

  public void cursorMoveTo(Vector2f screenPoint, Vector3f worldPoint) {
    if (tooltip != null) {
      tooltip.moveTo(this, screenPoint);
    }

    cursorPos.set(screenPoint);
  }

  public void onUnselect() {
    selected = false;
    cursorPos.set(-1);

    Plugin plugin = PluginUtil.getPlugin();
    findHoverText(display -> {
      player.hideEntity(plugin, display);
    });
  }

  private void findHoverText(Consumer<Display> operation) {
    if (tooltip == null) {
      return;
    }

    tooltip.forEachEntity(operation);
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
    Vector2f dim = bounds.getDimensions();

    this.root = new Node();
    this.tooltip = new Node();

    RenderElement rootRender = root.getRenderElement();
    rootRender.getPosition().set(dim);
    rootRender.getPaddingSize().set(0, 0, dim.y, dim.x);
    rootRender.setBackgroundColor(Color.GRAY);

    Node child = new Node();
    RenderElement childRender = child.getRenderElement();
    float yMargin = CHAR_PX_SIZE * 5.0f;
    childRender.setContent(new TextContent(Component.text("Hello, world!", NamedTextColor.YELLOW)));
    childRender.getContentScale().mul(1.5f);
    childRender.getMarginSize().z = yMargin;
    childRender.getMarginSize().y = yMargin;

    Node child2 = new Node();
    RenderElement c2r = child2.getRenderElement();
    c2r.setContent(new TextContent(Component.text("It basically works :)", NamedTextColor.GOLD)));
    c2r.setTextShadowed(true);

    Node child3 = new Node();
    RenderElement c3r = child3.getRenderElement();
    c3r.setContent(new TextContent(Component.text("I am a button :)")));
    c3r.setTextShadowed(true);
    c3r.getMarginSize().y = yMargin * 2;
    c3r.getMarginSize().x = -yMargin;
    c3r.getPaddingSize().set(CHAR_PX_SIZE);
    c3r.getOutlineSize().set(CHAR_PX_SIZE);

    root.addChild(child);
    root.addChild(child2);
    root.addChild(child3);


    RenderElement toolTipRender = tooltip.getRenderElement();
    toolTipRender.setOutlineColor(Color.GREEN);
    toolTipRender.setBackgroundColor(Color.BLACK);
    toolTipRender.setContent(new TextContent(Component.text("Hello, I'm a tooltip\n2nd Line", NamedTextColor.YELLOW)));
    toolTipRender.getOutlineSize().set(CHAR_PX_SIZE, CHAR_PX_SIZE, CHAR_PX_SIZE, CHAR_PX_SIZE);
    toolTipRender.getPaddingSize().set(CHAR_PX_SIZE * 2);
    toolTipRender.getContentScale().set(0.5f);
    toolTipRender.setTextShadowed(true);

    Node tooltipItem = new Node();
    RenderElement tiRender = tooltipItem.getRenderElement();
    tiRender.setContent(new ItemContent(new ItemStack(Material.TRIDENT, 1)));
    tiRender.setOutlineColor(Color.GREEN);
    tiRender.setBackgroundColor(Color.BLACK);
    tiRender.getContentScale().set(0.5f);

    tooltip.addChild(tooltipItem);

    root.spawn(this);
    root.align(this);

    tooltip.spawn(this);
    tooltip.align(this);
  }
}
