package net.arcadiusmc.ui.render;

import static net.arcadiusmc.ui.render.RenderElement.CHAR_PX_SIZE;
import static net.arcadiusmc.ui.render.RenderElement.NIL_COLOR;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.ui.render.RenderElement.Layer;
import net.arcadiusmc.ui.style.Rule;
import net.arcadiusmc.ui.style.StylePropertyMap;
import net.arcadiusmc.ui.style.Rules;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector2f;

public abstract class TextContent implements ElementContent {

  private static final Map<TextDecoration, Rule<Boolean>> DECORATION_RULE_MAP = Map.ofEntries(
      Map.entry(TextDecoration.ITALIC, Rules.ITALIC),
      Map.entry(TextDecoration.BOLD, Rules.BOLD),
      Map.entry(TextDecoration.STRIKETHROUGH, Rules.STRIKETHROUGH),
      Map.entry(TextDecoration.UNDERLINED, Rules.UNDERLINED),
      Map.entry(TextDecoration.OBFUSCATED, Rules.OBFUSCATED)
  );

  // 8 for character + 2 for descender space
  public static final float LINE_HEIGHT = CHAR_PX_SIZE * (8 + 2);

  @Override
  public Display createEntity(World world, Location location) {
    return world.spawn(location, TextDisplay.class, d -> d.setBackgroundColor(NIL_COLOR));
  }

  protected abstract Component getBaseText();

  protected boolean overrideStyle() {
    return true;
  }

  private Component resolve(StylePropertyMap set) {
    Component base = getBaseText();

    if (Text.isEmpty(base)) {
      return Component.empty();
    }

    Component result;
    boolean override = overrideStyle();
    TextColor textColor = Rules.toTextColor(set.get(Rules.TEXT_COLOR));

    if (override) {
      result = base.color(textColor);
    } else {
      result = base.colorIfAbsent(textColor);
    }

    for (Entry<TextDecoration, Rule<Boolean>> entry : DECORATION_RULE_MAP.entrySet()) {
      boolean state = set.get(entry.getValue());

      if (override) {
        result = result.decoration(entry.getKey(), State.byBoolean(state));
      } else {
        result = result.decorationIfAbsent(entry.getKey(), State.byBoolean(state));
      }
    }

    return result;
  }

  @Override
  public void applyContentTo(Display entity, StylePropertyMap set) {
    TextDisplay display = (TextDisplay) entity;

    Component text = resolve(set);
    display.text(text);
  }

  @Override
  public Class<? extends Display> getEntityClass() {
    return TextDisplay.class;
  }

  @Override
  public void measureContent(Vector2f out, StylePropertyMap set) {
    if (isEmpty()) {
      out.set(0);
      return;
    }

    Component text = resolve(set);
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
    return Text.isEmpty(getBaseText());
  }

  @Override
  public void configureInitial(Layer layer, RenderElement element) {

  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + Text.plain(getBaseText()).replace("\n", "\\n") + "]";
  }
}
