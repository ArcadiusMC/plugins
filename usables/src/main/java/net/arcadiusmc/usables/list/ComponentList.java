package net.arcadiusmc.usables.list;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;

import com.google.common.base.Strings;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.RecordBuilder;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.PrefixedWriter;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.AbstractListIterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public interface ComponentList<T extends UsableComponent> extends Iterable<T> {

  Logger LOGGER = Loggers.getLogger();

  Registry<ObjectType<T>> getRegistry();

  boolean isEmpty();

  int size();

  void clear();

  default void addFirst(T value) {
    add(value, 0);
  }

  default void addLast(T value) {
    add(value, size());
  }

  void add(T value, int index);

  void set(int index, T value);

  T remove(int index);

  void removeBetween(int fromIndex, int toIndex);

  default void write(TextWriter writer, String commandPrefix) {
    if (isEmpty()) {
      writer.write("empty");
      return;
    }

    if (!Strings.isNullOrEmpty(commandPrefix)) {
      writer.write(
          text("[clear] ", NamedTextColor.AQUA)
              .hoverEvent(text("Clears the list"))
              .clickEvent(ClickEvent.runCommand(commandPrefix + " clear"))
      );

      writer.write(
          text("[add]", NamedTextColor.GREEN)
              .hoverEvent(text("Suggests a command to add an element"))
              .clickEvent(ClickEvent.suggestCommand(commandPrefix + " add "))
      );

      writer.write(": ");
    }

    PrefixedWriter prefixed = writer.withIndent(2);

    for (int i = 0; i < size(); i++) {
      int viewIndex = i + 1;

      if (Strings.isNullOrEmpty(commandPrefix)) {
        if (!prefixed.isLineEmpty()) {
          prefixed.newLine();
        }
      } else {
        prefixed.line(
            text("(❌)", NamedTextColor.RED)
                .hoverEvent(text("Removes this element"))
                .clickEvent(ClickEvent.runCommand(commandPrefix + " remove " + viewIndex))
        );

        prefixed.space();
      }

      prefixed.write(viewIndex + ") ", NamedTextColor.GRAY);

      Component display = displayEntry(i, commandPrefix);
      prefixed.write(display);
    }
  }

  default Component displayType(ObjectType<T> type) {
    Component prefix;

    if (type == null) {
      prefix = text("TRANSIENT", NamedTextColor.YELLOW);
    } else {
      prefix = getRegistry().getKey(type)
          .map(s -> text(s, NamedTextColor.YELLOW))
          .orElseGet(() -> text("UNKNOWN", NamedTextColor.YELLOW));
    }

    return prefix;
  }

  default Component displayEntry(int index, String commandPrefix) {
    T component = get(index);
    ObjectType<T> type = (ObjectType<T>) component.getType();

    Component prefix = displayType(type);
    Component displayInfo = component.displayInfo();

    if (displayInfo == null) {
      return prefix;
    } else {
      return textOfChildren(prefix, text(": "), displayInfo);
    }
  }

  default ObjectType<T> getType(int index) {
    T value = get(index);
    return (ObjectType<T>) value.getType();
  }

  T get(int index);

  boolean contains(T value);

  int indexOf(T value);

  @SuppressWarnings("unchecked")
  default  <S> DataResult<S> save(DynamicOps<S> ops) {
    ListBuilder<S> builder = ops.listBuilder();
    Registry<ObjectType<T>> registry = getRegistry();

    var it = iterator();
    while (it.hasNext()) {
      int index = it.nextIndex();
      T t = it.next();

      ObjectType<T> type = (ObjectType<T>) t.getType();

      // Transient type
      if (type == null) {
        continue;
      }

      registry.getKey(type).ifPresentOrElse(key -> {
        RecordBuilder<S> mapBuilder = ops.mapBuilder();
        mapBuilder.add("type", ops.createString(key));

        type.save(t, ops)
            .mapError(s -> "Failed to save '" + key + "': " + s)
            .resultOrPartial(LOGGER::error)
            .filter(s -> !Objects.equals(s, ops.empty()))
            .ifPresent(s -> {
              mapBuilder.add("value", s);
              onSaved(index, mapBuilder);
            });

        builder.add(mapBuilder.build(ops.empty()));
      }, () -> {
        LOGGER.error("UsageType {} is not registered", type);
      });
    }

    return builder.build(ops.emptyList());
  }

  default <S> void onSaved(int index, RecordBuilder<S> builder) {

  }

  default <S> void load(Dynamic<S> dynamic) {
    clear();

    List<Dynamic<S>> dynamicList = dynamic.asList(Function.identity());

    if (dynamicList.isEmpty()) {
      return;
    }

    Registry<ObjectType<T>> registry = getRegistry();

    for (Dynamic<S> element : dynamicList) {
      Optional<String> keyOptional = element.get("type").asString()
          .mapError(s -> "Error getting 'type': " + s)
          .resultOrPartial(LOGGER::error);

      if (keyOptional.isEmpty()) {
        continue;
      }

      String key = keyOptional.get();

      registry.get(key).ifPresentOrElse(type -> {
        Dynamic<S> valueElement = element.get("value")
            .result()
            .orElse(null);

        if (valueElement == null) {
          try {
            var value = type.createEmpty();
            addLast(value);
            onLoad(size()-1, element, value);
          } catch (UnsupportedOperationException exc) {
            LOGGER.error("Type '{}' doesn't support createEmpty() but has no data to load",
                key, exc
            );
          }

          return;
        }

        type.load(valueElement)
            .mapError(s -> "Error loading " + key + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(t -> {
              addLast(t);
              onLoad(size()-1, element, t);
            });

      }, () -> {
        LOGGER.error("Couldn't find component with key '{}'", key);
      });
    }
  }

  default <S> void onLoad(int index, Dynamic<S> dynamic, T value) {

  }

  @NotNull
  @Override
  default ListIterator<T> iterator() {
    return new ComponentListIterator<>(this);
  }

  class ComponentListIterator<T extends UsableComponent> extends AbstractListIterator<T> {

    final ComponentList<T> list;

    public ComponentListIterator(ComponentList<T> list) {
      this.list = list;
    }

    @Override
    protected void add(int pos, T val) {
      list.add(val, pos);
    }

    @Override
    protected @Nullable T get(int pos) {
      return list.get(pos);
    }

    @Override
    protected void set(int pos, @Nullable T val) {
      list.set(pos, val);
    }

    @Override
    protected void remove(int pos) {
      list.remove(pos);
    }

    @Override
    protected int size() {
      return list.size();
    }
  }
}
