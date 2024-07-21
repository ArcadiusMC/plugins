package net.arcadiusmc.ui.render;

import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.style.StylePropertyMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.joml.Vector2f;

public interface ElementContent {

  Display createEntity(World world, Location location);

  void applyContentTo(Display entity, StylePropertyMap set);

  Class<? extends Display> getEntityClass();

  void measureContent(Vector2f out, StylePropertyMap set);

  boolean isEmpty();

  void configureInitial(Layer layer, RenderElement element);
}
