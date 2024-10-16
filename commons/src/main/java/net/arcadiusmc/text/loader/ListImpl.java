package net.arcadiusmc.text.loader;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.parse.ChatParseFlag;
import net.arcadiusmc.text.parse.ChatParser;
import net.arcadiusmc.text.parse.TextContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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
    entry.set(format);

    return this;
  }

  @Override
  public void add(String key, Component[] array, ListMode mode) {
    Objects.requireNonNull(key, "Null key");
    Objects.requireNonNull(array, "Null array");
    Objects.requireNonNull(mode, "Null mode");

    MessageEntry entry = entries.computeIfAbsent(key, MessageEntry::new);
    entry.set(array);
    entry.listMode = mode;
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
  public MessageList getChild(@NotNull String childKey) {
    Objects.requireNonNull(childKey, "Null key");
    if (children == null) {
      return null;
    }

    return children.get(childKey);
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

  @Override
  public Set<String> keys() {
    return Collections.unmodifiableSet(entries.keySet());
  }

  @Override
  public Set<String> childKeys() {
    if (children == null) {
      return Set.of();
    }
    return Collections.unmodifiableSet(children.keySet());
  }

  private static class MessageEntry implements MessageRef {

    static final Random RANDOM = new Random();

    private final String key;

    private Component[] value = null;
    private ListMode listMode = ListMode.ITERATING;
    private int idx = 0;

    private final Component[] lenOne = new Component[1];

    public MessageEntry(String key) {
      this.key = key;
    }

    public void set(Component value) {
      lenOne[0] = value;
      this.value = lenOne;
      this.idx = 0;
    }

    public void set(Component[] value) {
      this.value = value;
      this.idx = 0;
    }

    @Override
    public MessageRender get() {
      if (value == null || value.length < 1) {
        return new NullRender(key);
      }

      if ("server.chat".equals(key)) {
        Logger l = Loggers.getLogger();
        l.debug("get message render call: value.len={}, listMode={}, idx={}",
            value.length, listMode, idx,
            new Throwable()
        );
      }

      if (value.length == 1) {
        return new FormatRender(value[0]);
      }

      Component text;

      switch (listMode) {
        case RANDOM:
          text = value[RANDOM.nextInt(value.length)];
          break;

        case SHUFFLE:
          text = value[idx++];
          if (idx >= value.length) {
            idx = 0;
            ObjectArrays.shuffle(value, RANDOM);
          }
          break;

        case null:
        default:
          text = value[idx++];
          if (idx >= value.length) {
            idx = 0;
          }
          break;
      }

      return new FormatRender(text);
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
