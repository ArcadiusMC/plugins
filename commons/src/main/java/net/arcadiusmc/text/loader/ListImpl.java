package net.arcadiusmc.text.loader;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.arcadiusmc.text.parse.ChatParseFlag;
import net.arcadiusmc.text.parse.ChatParser;
import net.arcadiusmc.text.parse.TextContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ListImpl implements MessageList {

  static final Set<ChatParseFlag> FLAGS = Set.of(
      ChatParseFlag.COLORS,
      ChatParseFlag.GRADIENTS,
      ChatParseFlag.EMOJIS,
      ChatParseFlag.IGNORE_SWEARS,
      ChatParseFlag.IGNORE_CASE,
      ChatParseFlag.TIMESTAMPS,
      ChatParseFlag.TAGGING
  );

  static final TextContext TEXT_CONTEXT = TextContext.of(FLAGS, null);

  private final Map<String, MessageEntry> entries = new Object2ObjectOpenHashMap<>();

  private Map<String, ListImpl> children;

  private static Component toBaseFormat(String str) {
    return ChatParser.parser().parseBasic(str, TEXT_CONTEXT);
  }

  @Override
  public MessageList add(@NotNull String key, @NotNull String format) {
    Objects.requireNonNull(format, "Null format");
    return add(key, toBaseFormat(format));
  }

  @Override
  public MessageList add(@NotNull String key, @NotNull Component format) {
    Objects.requireNonNull(key, "Null key");
    Objects.requireNonNull(format, "Null format");

    MessageEntry entry = entries.computeIfAbsent(key, MessageEntry::new);
    entry.value = format;

    return this;
  }

  @Override
  public MessageRef reference(@NotNull String key) {
    Objects.requireNonNull(key, "Null key");
    MessageEntry result = findRef(key);

    if (result != null) {
      return result;
    }

    result = new MessageEntry(key);
    entries.put(key, result);

    return result;
  }

  private MessageEntry findRef(String key) {
    MessageEntry result = entries.get(key);

    if (result != null) {
      return result;
    }

    if (children == null || children.isEmpty()) {
      return null;
    }

    for (ListImpl child : children.values()) {
      result = child.findRef(key);

      if (result == null) {
        continue;
      }

      return result;
    }

    return null;
  }

  @Override
  public MessageRender render(@NotNull String key) {
    Objects.requireNonNull(key, "Null key");

    MessageEntry entry = findRef(key);

    if (entry == null) {
      return new NullRender(key);
    }

    return entry.get();
  }

  @Override
  public void clear() {
    entries.forEach((string, messageEntry) -> {
      messageEntry.value = null;
    });
  }

  @Override
  public boolean hasMessage(String key) {
    Objects.requireNonNull(key, "Null key");
    return findRef(key) != null;
  }

  @Override
  public MessageList addChild(@NotNull String childKey, @NotNull MessageList childList) {
    Objects.requireNonNull(childKey, "Null key");
    Objects.requireNonNull(childList, "Null child list");

    if (children == null) {
      children = new HashMap<>();
    }

    children.put(childKey, (ListImpl) childList);
    return this;
  }

  @Override
  public boolean removeChild(@NotNull String childKey) {
    Objects.requireNonNull(childKey, "Null key");

    if (children == null) {
      return false;
    }

    return children.remove(childKey) != null;
  }

  private static class MessageEntry implements MessageRef {

    private final String key;
    private Component value;

    public MessageEntry(String key) {
      this.key = key;
    }

    public Optional<Component> getText() {
      return Optional.ofNullable(value);
    }

    @Override
    public MessageRender get() {
      if (value == null) {
        return new NullRender(key);
      }

      return new FormatRender(value);
    }

    @Override
    public Component renderText(@Nullable Audience viewer) {
      return get().create(viewer);
    }

    @Override
    public CommandSyntaxException exception(@Nullable Audience viewer) {
      return get().exception(viewer);
    }
  }
}
