package net.arcadiusmc.ui.render;

import static net.arcadiusmc.ui.render.RenderElement.CHAR_PX_SIZE;
import static net.arcadiusmc.ui.render.RenderElement.NIL_COLOR;

import java.util.List;
import java.util.Objects;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector2f;

public class TextContent implements ElementContent {

  // 8 for character + 2 for descender space
  public static final float LINE_HEIGHT = CHAR_PX_SIZE * (8 + 2);

  private final Component text;

  public TextContent(Component text) {
    this.text = Objects.requireNonNull(text, "Null text");
  }

  @Override
  public Display createEntity(World world, Location location) {
    TextDisplay display = world.spawn(location, TextDisplay.class);

    display.text(text);
    display.setBackgroundColor(NIL_COLOR);

    return display;
  }

  @Override
  public void measureContent(Vector2f out) {
    if (Text.isEmpty(text)) {
      out.set(0);
      return;
    }

    List<Component> lines = Text.splitNewlines(text);
    int longest = 0;

    for (Component line : lines) {
      int length = TextInfo.length(line);
      longest = Math.max(longest, length);
    }

    out.x = longest * CHAR_PX_SIZE;
    out.y = lines.size() * LINE_HEIGHT;
  }

  @Override
  public boolean isEmpty() {
    return Text.isEmpty(text);
  }

  @Override
  public void configureInitial(Layer layer, RenderElement element) {

  }
}
