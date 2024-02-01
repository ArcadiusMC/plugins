package net.arcadiusmc.text.loader;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * List of loaded message formats
 * @see MessageLoader
 */
public interface MessageList {

  /**
   * Creates a new message list
   * @return New message list
   */
  static MessageList create() {
    return new ListImpl();
  }

  MessageList add(@NotNull String key, @NotNull String format);

  MessageList add(@NotNull String key, @NotNull Component format);

  MessageRef reference(@NotNull String key);

  MessageRender render(@NotNull String key);

  default Component renderText(@NotNull String key, @Nullable Audience viewer) {
    return render(key).create(viewer);
  }

  default CommandSyntaxException exception(@NotNull String key, @Nullable Audience viewer) {
    return render(key).exception(viewer);
  }

  void clear();

  MessageList addChild(@NotNull String childKey, @NotNull MessageList childList);

  boolean removeChild(@NotNull String childKey);
}
