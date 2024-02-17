package net.arcadiusmc.core.placeholder;

import com.google.common.base.Strings;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class KeybindPlaceholder implements TextPlaceholder {

  @Override
  public @Nullable Component render(String match, PlaceholderContext render) {
    if (Strings.isNullOrEmpty(match)) {
      return null;
    }

    return Component.keybind(match);
  }
}
