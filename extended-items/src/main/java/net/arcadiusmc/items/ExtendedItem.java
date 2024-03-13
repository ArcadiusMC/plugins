package net.arcadiusmc.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.BufferedTextWriter;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ExtendedItem {

  static final String TAG_CONTAINER = "extended_item";
  static final String TAG_TYPE_KEY = "type";
  static final String TAG_DATA = "data";

  private final Holder<ItemType> type;

  private final ItemStack handle;
  private final ItemMeta meta;

  private List<ItemComponent> components;
  private List<LoreElement> loreElements;
  private boolean componentsLocked = false;

  public ExtendedItem(Holder<ItemType> type, ItemStack handle) {
    this.type = type;
    this.handle = handle;
    this.meta = handle.getItemMeta();
  }

  public void init() {
    if (components == null) {
      return;
    }

    for (ItemComponent component : components) {
      component.onInit();
    }

    componentsLocked = true;
  }

  public void update() {
    applyUpdates(handle, meta);

    BufferedTextWriter writer = TextWriters.buffered();
    writeLore(writer);
    meta.lore(writer.getBuffer());

    save(meta);

    handle.setItemMeta(meta);
  }

  private void save(ItemMeta meta) {
    CompoundTag tag = BinaryTags.compoundTag();

    if (components != null) {
      for (ItemComponent component : components) {
        component.save(tag);
      }
    }

    CompoundTag containerTag = BinaryTags.compoundTag();
    containerTag.put(TAG_DATA, tag);
    containerTag.putString(TAG_TYPE_KEY, type.getKey());

    ItemStacks.setTagElement(meta, TAG_CONTAINER, containerTag);
  }

  void load(CompoundTag tag) {
    if (components == null || components.isEmpty()) {
      return;
    }

    for (ItemComponent component : components) {
      component.load(tag);
    }
  }

  private void applyUpdates(ItemStack stack, ItemMeta meta) {
    if (components == null) {
      return;
    }
    for (ItemComponent component : components) {
      component.onUpdate(meta, stack);
    }
  }

  private void writeLore(TextWriter writer) {
    if (loreElements == null) {
      return;
    }

    for (LoreElement component : loreElements) {
      component.writeLore(this, writer);
    }
  }

  public <T> List<T> getMatching(Class<T> type) {
    if (components == null) {
      return List.of();
    }

    List<T> result = null;

    for (ItemComponent component : components) {
      if (!type.isInstance(component)) {
        continue;
      }

      if (result == null) {
        result = new ArrayList<>();
      }

      result.add(type.cast(component));
    }

    return result == null ? List.of() : Collections.unmodifiableList(result);
  }

  public void addLore(@NotNull LoreElement loreElement) {
    Objects.requireNonNull(loreElement, "Null lore element");

    if (componentsLocked) {
      throw new IllegalStateException("Components locked");
    }

    if (loreElements == null) {
      loreElements = new ArrayList<>();
    }

    loreElements.add(loreElement);
  }

  public void addComponent(@NotNull ItemComponent component) {
    Objects.requireNonNull(component, "Null component");

    if (componentsLocked) {
      throw new IllegalStateException("Components locked");
    }

    if (component.item != null) {
      throw new IllegalStateException("Component already bound to different item");
    }

    if (components == null) {
      components = new ArrayList<>();
    }

    components.add(component);
    component.setItem(this);
  }

  public <T> Optional<T> getComponent(Class<T> type) {
    if (components == null) {
      return Optional.empty();
    }

    for (ItemComponent component : components) {
      if (type.isInstance(component)) {
        return Optional.of(type.cast(component));
      }
    }

    return Optional.empty();
  }
}
