package net.arcadiusmc.ui.render;

import static net.arcadiusmc.ui.render.RenderElement.GLOBAL_SCALAR;
import static net.arcadiusmc.ui.render.RenderElement.ITEM_SPRITE_SIZE;

import java.util.Objects;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.style.StylePropertyMap;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector2f;

public class ItemContent implements ElementContent {

  public static final float Z_SCALE = 0.150f * GLOBAL_SCALAR;
  public static final float Z_OFF = 0.033f * GLOBAL_SCALAR;
  public static final float Y_OFF_MODIFIER = 0.5f;
  public static final float ROTATION = (float) Math.toRadians(180);

  private final ItemStack item;

  public ItemContent(ItemStack item) {
    this.item = Objects.requireNonNull(item, "Null item");
  }

  @Override
  public Display createEntity(World world, Location location) {
    ItemDisplay display = world.spawn(location, ItemDisplay.class);
    display.setItemDisplayTransform(ItemDisplayTransform.GUI);
    return display;
  }

  @Override
  public void applyContentTo(Display entity, StylePropertyMap set) {
    ItemDisplay display = (ItemDisplay) entity;
    display.setItemStack(item);
  }

  @Override
  public Class<? extends Display> getEntityClass() {
    return ItemDisplay.class;
  }

  @Override
  public void measureContent(Vector2f out, StylePropertyMap set) {
    if (ItemStacks.isEmpty(item)) {
      out.set(0);
      return;
    }

    out.set(ITEM_SPRITE_SIZE);
  }

  @Override
  public boolean isEmpty() {
    return ItemStacks.isEmpty(item);
  }

  @Override
  public void configureInitial(Layer layer, RenderElement element) {
    layer.scale.z = Z_SCALE;

    float transY = layer.size.y * Y_OFF_MODIFIER * element.getContentScale().y * GLOBAL_SCALAR;
    layer.translate.y += transY;
    layer.translate.z -= Z_OFF;
    layer.leftRotation.rotateY(ROTATION);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + ItemStacks.toNbtString(item) + "]";
  }
}
