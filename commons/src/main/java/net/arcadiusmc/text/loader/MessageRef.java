package net.arcadiusmc.text.loader;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to a loaded message
 * @see MessageList#reference(String)
 */
public interface MessageRef {

  /**
   * Gets the referenced message
   * @return Referenced message
   */
  MessageRender get();

  /**
   * Render the referenced message using the specified viewer
   * @param viewer Message viewer
   * @return Rendered message
   */
  Component renderText(@Nullable Audience viewer);

  /**
   * Creates a syntax exception from the referenced message
   * @param viewer Message viewer
   * @return Rendered exception
   */
  CommandSyntaxException exception(@Nullable Audience viewer);

  default CommandSyntaxException exception() {
    return exception(null);
  }
}
