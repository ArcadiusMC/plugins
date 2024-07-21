package net.arcadiusmc.ui.render;

import java.util.Objects;
import net.kyori.adventure.text.Component;

public class ComponentContent extends TextContent {

  private final Component content;

  public ComponentContent(Component content) {
    Objects.requireNonNull(content, "Null content");
    this.content = content;
  }

  @Override
  protected boolean overrideStyle() {
    return false;
  }

  @Override
  protected Component getBaseText() {
    return content;
  }
}
