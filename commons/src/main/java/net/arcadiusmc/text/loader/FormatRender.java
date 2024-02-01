package net.arcadiusmc.text.loader;

import java.util.HashMap;
import java.util.Map;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.parse.ChatParser;
import net.arcadiusmc.text.parse.TextContext;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class FormatRender implements MessageRender, ViewerAwareMessage {

  private final Component baseFormat;

  private final PlaceholderRenderer renderer;
  private Map<String, Object> variables = null;

  public FormatRender(Component format) {
    this.baseFormat = format;
    this.renderer = Placeholders.newRenderer().useDefaults();
  }

  @Override
  public MessageRender addValue(@NotNull String key, @Nullable Object value) {
    if (key.contains(".")) {
      renderer.add(key, value);
      return this;
    }

    if (variables == null) {
      variables = new HashMap<>();
    }

    variables.put(key, value);
    return this;
  }

  @Override
  public Component create(@Nullable Audience viewer) {
    TextContext ctx = TextContext.of(ListImpl.FLAGS, viewer);
    Component format = ChatParser.parser().runFunctions(baseFormat, ctx);
    return renderer.render(format, viewer, variables == null ? Map.of() : variables);
  }
}
