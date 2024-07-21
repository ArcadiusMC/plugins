package net.arcadiusmc.ui.render;

import com.google.common.base.Strings;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public class StringContent extends TextContent {

  private final String text;

  public StringContent(String text) {
    Objects.requireNonNull(text, "Null text");
    this.text = text;
  }

  @Override
  protected Component getBaseText() {
    return Component.text(text);
  }

  @Override
  public boolean isEmpty() {
    return Strings.isNullOrEmpty(text);
  }
}
