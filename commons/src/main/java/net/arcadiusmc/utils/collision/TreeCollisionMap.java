package net.arcadiusmc.utils.collision;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.utils.math.AbstractBounds3i;
import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class TreeCollisionMap<T> implements CollisionLookup<T>, SpatialMap<T> {

  // 9 makes a cell size of 512, also the same size as an MCA region
  static final int DEFAULT_CELL_BITSHIFT = 9;

  private final Map<T, Entry<T>> entryMap = new Object2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Cell<T>> cellMap = new Long2ObjectOpenHashMap<>();

  @Getter
  private final int rootCellBitshift;

  public TreeCollisionMap() {
    this(DEFAULT_CELL_BITSHIFT);
  }

  public TreeCollisionMap(int rootCellBitshift) {
    this.rootCellBitshift = rootCellBitshift;
  }

  @Override
  public boolean add(@NotNull T value, @NotNull Bounds3i bounds) {
    Objects.requireNonNull(value, "Null value");
    Objects.requireNonNull(bounds, "Null bounds");

    Entry<T> existing = entryMap.get(value);

    if (existing != null) {
      if (existing.boundingBox.equals(bounds)) {
        return false;
      }

      removeEntry(existing);
    } else {
      existing = new Entry<>();
    }

    existing.boundingBox = bounds;
    existing.value = value;



    return true;
  }

  @Override
  public boolean remove(@NotNull T value) {
    Objects.requireNonNull(value, "Null value");
    Entry<T> entry = entryMap.get(value);

    if (entry == null) {
      return false;
    }

    removeEntry(entry);
    return true;
  }

  @Override
  public @NotNull Set<T> getOverlapping(@NotNull AbstractBounds3i<?> bounds3i) {
    return null;
  }

  @Override
  public @NotNull Set<T> get(@NotNull Vector3i pos) {
    return null;
  }

  @Override
  public @NotNull ObjectDoublePair<T> findNearest(@NotNull Vector3d point) {
    return null;
  }

  @Override
  public void clear() {
    entryMap.clear();
    cellMap.clear();
  }

  @Override
  public int size() {
    return entryMap.size();
  }

  @Override
  public boolean isEmpty() {
    return entryMap.isEmpty();
  }

  @Override
  public Set<T> values() {
    return Collections.unmodifiableSet(entryMap.keySet());
  }

  private void removeEntry(Entry<T> entry) {

  }

  @Override
  public void getColliding(World world, Bounds3i bounds3i, CollisionSet<T> out) {

  }

  static abstract class Cell<T> implements CollisionLookup<T> {

    abstract void push(Entry<T> entry);

    abstract void remove(Entry<T> value);

    abstract void getOverlapping(Bounds3i bounds3i, Set<T> output);

    abstract void get(Vector3i pos, Set<T> output);
  }

  static class SubdivisionCell<T> extends Cell<T> {
    static final int CHILD_COUNT = 8;
    static final int X_CELLS = 2;
    static final int Z_CELLS = 2;
    static final int Y_CELLS = 2;
    static final int XY_CELLS = X_CELLS * Y_CELLS;
    static final int MIN_BITSHIFT = 4;
    static final int MAX_CELL_ENTRIES = 10;

    final Cell<T>[] children;
    int totalEntryCount = 0;

    final Bounds3i cellBounds;
    final int bitshift;
    final int childBitshift;
    final int childSize;
    final int subdivisionSizeSq;

    final Cell<T>[] lookupbuf;
    final int[] indexbuf;

    SubdivisionCell(int offX, int offY, int offZ, int bitshift) {
      this.bitshift = bitshift;
      int size = 1 << bitshift;
      this.cellBounds = new Bounds3i(0, 0, 0, size, size, size).move(offX, offY, offZ);
      this.subdivisionSizeSq = cellBounds.sizeX() * cellBounds.sizeY();

      this.childBitshift = bitshift - 1;
      this.childSize = 1 << this.childBitshift;

      this.children = new Cell[CHILD_COUNT];
      this.lookupbuf = new Cell[CHILD_COUNT];
      this.indexbuf = new int[CHILD_COUNT];
    }

    private int cellsOf(Bounds3i bounds3i) {
      Bounds3i intersection = bounds3i
          .intersection(cellBounds)
          .move(-cellBounds.minX(), -cellBounds.minY(), -cellBounds.minZ());

      // Min cell pos
      int minX = intersection.minX() >> bitshift;
      int minY = intersection.minY() >> bitshift;
      int minZ = intersection.minZ() >> bitshift;

      // Max cell pos
      int maxX = intersection.maxX() >> bitshift;
      int maxY = intersection.maxY() >> bitshift;
      int maxZ = intersection.maxZ() >> bitshift;

      int bufindex = 0;

      // Loop through all chunks
      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          for (int z = minZ; z <= maxZ; z++) {
            int index = x + y * X_CELLS + z * XY_CELLS;

            indexbuf[bufindex] = index;
            lookupbuf[bufindex++] = children[index];
          }
        }
      }

      return bufindex;
    }

    @Override
    void push(Entry<T> entry) {
      totalEntryCount++;
      int len = cellsOf(entry.boundingBox);

      for (int i = 0; i < len; i++) {
        Cell<T> cell = lookupbuf[i];

        if (cell == null) {
          int cellindex = indexbuf[i];
          cell = new HolderCell<>();
          children[cellindex] = cell;
        } else if (cell instanceof HolderCell<T> holderCell
            && holderCell.length >= MAX_CELL_ENTRIES
            && bitshift > MIN_BITSHIFT
        ) {
          int cellindex = indexbuf[i];
          cell = subdivideCell(cellindex);
        }

        cell.push(entry);
      }
    }

    private Cell<T> subdivideCell(int index) {
      HolderCell<T> existing = (HolderCell<T>) children[index];

      int offX = cellBounds.minX() + childSize * index % X_CELLS;
      int offY = cellBounds.minY() + childSize * (index / X_CELLS) % Y_CELLS;
      int offZ = cellBounds.minZ() + childSize * index / XY_CELLS;

      SubdivisionCell<T> subdivisionCell = new SubdivisionCell<>(offX, offY, offZ, childBitshift);
      children[index] = subdivisionCell;

      for (Entry<T> entry : existing.entries) {
        subdivisionCell.push(entry);
      }

      return subdivisionCell;
    }

    @Override
    void remove(Entry<T> value) {
      totalEntryCount--;
      int len = cellsOf(value.boundingBox);

      for (int i = 0; i < len; i++) {
        Cell<T> cell = lookupbuf[i];

        if (cell == null) {
          continue;
        }

        cell.remove(value);
      }
    }

    @Override
    void getOverlapping(Bounds3i bounds3i, Set<T> output) {

    }

    @Override
    void get(Vector3i pos, Set<T> output) {

    }

    @Override
    public void getColliding(World world, Bounds3i bounds3i, CollisionSet<T> out) {
      if (totalEntryCount == 0) {
        return;
      }

      if (bounds3i.contains(cellBounds)) {
        for (Cell<T> child : children) {
          if (child == null) {
            continue;
          }

          child.getColliding(world, bounds3i, out);
        }

        return;
      }

      int len = cellsOf(bounds3i);
      if (len < 0) {
        return;
      }

      for (int i = 0; i < len; i++) {
        int index = indexbuf[i];
        Cell<T> cell = children[index];
        cell.getColliding(world, bounds3i, out);
      }
    }
  }

  static class HolderCell<T> extends Cell<T> {

    Entry<T>[] entries;
    int length = 0;

    @Override
    void push(Entry<T> entry) {
      if (length == 0) {
        entries = new Entry[10];
        length = 1;
        entries[0] = entry;

        return;
      }

      if (length == entries.length) {
        var old = entries;
        entries = new Entry[entries.length + 10];
        System.arraycopy(old, 0, entries, 0, old.length);
      }

      entries[length++] = entry;
    }

    @Override
    void remove(Entry<T> value) {
      int index = indexOf(value.value);
      entries[index] = null;

      if (index < length) {
        System.arraycopy(entries, index + 1, entries, index, entries.length - index);
      }

      length--;
    }

    int indexOf(T value) {
      for (int i = 0; i < entries.length; i++) {
        Entry<T> entry = entries[i];

        if (!Objects.equals(value, entry.value)) {
          continue;
        }

        return i;
      }

      return -1;
    }

    @Override
    void getOverlapping(Bounds3i bounds3i, Set<T> output) {
      if (length == 0) {
        return;
      }

      for (int i = 0; i < length; i++) {
        Entry<T> entry = entries[i];

        if (!entry.boundingBox.overlaps(bounds3i)) {
          continue;
        }

        output.add(entry.value);
      }
    }

    @Override
    void get(Vector3i pos, Set<T> output) {
      if (length == 0) {
        return;
      }

      for (int i = 0; i < length; i++) {
        Entry<T> entry = entries[i];

        if (!entry.boundingBox.contains(pos)) {
          continue;
        }

        output.add(entry.value);
      }
    }

    @Override
    public void getColliding(World world, Bounds3i bounds3i, CollisionSet<T> out) {
      if (length == 0) {
        return;
      }

      for (int i = 0; i < length; i++) {
        Entry<T> entry = entries[i];

        if (!entry.boundingBox.overlaps(bounds3i)) {
          continue;
        }

        out.add(new Collision<>(entry.value, world, entry.boundingBox));
      }
    }
  }

  static class Entry<T> {
    T value;
    Bounds3i boundingBox;
  }
}
