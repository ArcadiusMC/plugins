package net.arcadiusmc.ui.render;

import net.arcadiusmc.ui.render.RenderElement.Layer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.joml.Vector2f;

public interface ElementContent {

  Display createEntity(World world, Location location);

  void measureContent(Vector2f out);

  boolean isEmpty();

  void configureInitial(Layer layer, RenderElement element);
}
