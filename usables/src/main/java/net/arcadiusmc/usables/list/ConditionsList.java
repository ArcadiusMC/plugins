package net.arcadiusmc.usables.list;

import static net.kyori.adventure.text.Component.text;

import com.google.common.base.Strings;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.Objects;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ConditionsList implements ComponentList<Condition> {

  static final Entry[] EMPTY_ARRAY = new Entry[0];

  Entry[] entries = EMPTY_ARRAY;
  int size;

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Registry<ObjectType<Condition>> getRegistry() {
    return (Registry) UsablesPlugin.get().getConditions();
  }

  public void setError(int index, String error) {
    Objects.checkIndex(index, size);
    entries[index].errorMessage = error;
  }

  public String getError(int index) {
    Objects.checkIndex(index, size);
    return entries[index].errorMessage;
  }

  public void clearErrorMessages() {
    for (int i = 0; i < size; i++) {
      Entry entry = entries[i];
      entry.errorMessage = null;
    }
  }

  private void grow() {
    grow(size + 1);
  }

  private void grow(int newSize) {
    if (entries.length >= newSize) {
      return;
    }

    entries = ObjectArrays.grow(entries, newSize);
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    Arrays.fill(entries, null);
    size = 0;
  }

  @Override
  public void add(Condition value, int index) {
    Objects.requireNonNull(value, "Null value");
    Objects.checkIndex(index, size + 1);

    grow();

    if (index != size) {
      System.arraycopy(entries, index, entries, index + 1, size - index);
    }

    Entry entry = new Entry();
    entry.condition = value;

    entries[index] = entry;
    size++;
  }

  @Override
  public void set(int index, Condition value) {
    Objects.requireNonNull(value, "Null value");
    Objects.checkIndex(index, size);

    Entry entry = entries[index];
    entry.condition = value;
  }

  @Override
  public Condition remove(int index) {
    Objects.checkIndex(index, size);

    Entry removed = entries[index];
    size--;

    if (index != size) {
      System.arraycopy(entries, index + 1, entries, index, size - index);
    }

    entries[size] = null;
    return removed.condition;
  }

  @Override
  public void removeBetween(int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, size);
    int removeObjects = toIndex - fromIndex;
    for (int i = 0; i < removeObjects; i++) {
      remove(fromIndex);
    }
  }

  @Override
  public Condition get(int index) {
    Objects.checkIndex(index, size);
    Entry entry = entries[index];
    return entry.condition;
  }

  @Override
  public boolean contains(Condition value) {
    return indexOf(value) != -1;
  }

  @Override
  public int indexOf(Condition value) {
    for (int i = 0; i < size; i++) {
      Entry entry = entries[i];

      if (!Objects.equals(value, entry.condition)) {
        continue;
      }

      return i;
    }

    return -1;
  }

  @Override
  public Component displayEntry(int index, String commandPrefix) {
    Component text = ComponentList.super.displayEntry(index, commandPrefix);
    String error = getError(index);

    TextColor color = Strings.isNullOrEmpty(error)
        ? NamedTextColor.GRAY
        : NamedTextColor.YELLOW;

    int vIndex = index + 1;

    String unprefixedCommand = unprefixCommand(commandPrefix);

    Component errorPrefix = text("[E] ", color)
        .hoverEvent(
            Text.format(
                """
                &eError message override:&r
                &7Message:&r {0}
                &7Shift-Click to change error message.
                Click to remove error message.""",

                error
            )
        )
        .insertion(unprefixedCommand + " error-overrides set " + vIndex + " " + Strings.nullToEmpty(error))
        .clickEvent(ClickEvent.runCommand(unprefixedCommand + " error-overrides remove " + vIndex));

    return Component.textOfChildren(errorPrefix, text);
  }

  private String unprefixCommand(String commandPrefix) {
    int lastIndex = commandPrefix.lastIndexOf(" tests");

    if (lastIndex == -1) {
      return commandPrefix;
    }

    return commandPrefix.substring(0, lastIndex);
  }

  @Override
  public <S> void onSaved(int index, RecordBuilder<S> builder) {
    String error = getError(index);

    if (Strings.isNullOrEmpty(error)) {
      return;
    }

    builder.add("error_message", builder.ops().createString(error));
  }

  @Override
  public <S> void onLoad(int index, Dynamic<S> dynamic, Condition value) {
    S gotten = dynamic.getElement("error_message", null);
    if (gotten == null) {
      return;
    }

    dynamic.getOps().getStringValue(gotten)
        .mapError(s -> "Couldn't load error_message at index " + index + ": " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(s -> setError(index, s));
  }

  static class Entry {
    Condition condition;
    String errorMessage;
  }
}
