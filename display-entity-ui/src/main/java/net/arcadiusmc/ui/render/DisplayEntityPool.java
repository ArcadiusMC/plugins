package net.arcadiusmc.ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public class DisplayEntityPool {

  private final World world;

  private final List<PoolEntry<ItemDisplay>> itemDisplays = new ArrayList<>();
  private final List<PoolEntry<TextDisplay>> textDisplays = new ArrayList<>();

  public DisplayEntityPool(World world) {
    this.world = world;
  }

  public ItemDisplay getItemDisplay() {
    return get(ItemDisplay.class, itemDisplays);
  }

  public TextDisplay getTextDisplay() {
    return get(TextDisplay.class, textDisplays);
  }

  public void freeItem(ItemDisplay display) {
    free(display, itemDisplays);
  }

  public void freeText(TextDisplay display) {
    free(display, textDisplays);
  }

  private <T extends Display> T get(Class<T> type, List<PoolEntry<T>> list) {
    for (PoolEntry<T> n : list) {
      if (!n.free) {
        continue;
      }

      n.free = false;
      return n.entity;
    }

    PoolEntry<T> entry = new PoolEntry<>();
    T entity = world.spawn(new Location(world, 0, 0, 0), type);

    entry.entity = entity;
    entry.free = false;

    list.add(entry);

    return entity;
  }

  private <T extends Display> void free(T value, List<PoolEntry<T>> list) {
    for (PoolEntry<T> entry : list) {
      if (!Objects.equals(value, entry.entity)) {
        continue;
      }

      entry.free = true;
      entry.entity.teleport(new Location(world, 0, 0, 0));
    }
  }

  private class PoolEntry<T extends Display> {
    T entity;
    boolean free;
  }
}
