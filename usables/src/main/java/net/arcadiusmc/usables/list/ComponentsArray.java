package net.arcadiusmc.usables.list;


import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.usables.UsableComponent;
import org.slf4j.Logger;

public abstract class ComponentsArray<T extends UsableComponent> implements ComponentList<T> {

  public static final Logger LOGGER = Loggers.getLogger();

  private T[] contents;
  private int size;

  @SuppressWarnings("unchecked")
  protected ComponentsArray(Class<T> arrayType) {
    Objects.requireNonNull(arrayType, "Null Array type");

    this.size = 0;
    this.contents = (T[]) Array.newInstance(arrayType, 0);
  }

  private void grow() {
    grow(size + 1);
  }

  private void grow(int newSize) {
    if (contents.length >= newSize) {
      return;
    }

    contents = ObjectArrays.grow(contents, newSize);
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
    Arrays.fill(contents, null);
    size = 0;
  }

  @Override
  public void add(T value, int index) {
    Objects.requireNonNull(value, "Null value");
    Objects.checkIndex(index, size + 1);

    grow();

    if (index != size) {
      System.arraycopy(contents, index, contents, index + 1, size - index);
    }

    contents[index] = value;
    size++;
  }

  @Override
  public void set(int index, T value) {
    Objects.requireNonNull(value, "Null value");
    Objects.checkIndex(index, size);
    contents[index] = value;
  }

  @Override
  public T remove(int index) {
    Objects.checkIndex(index, size);

    T removed = contents[index];
    size--;

    if (index != size) {
      System.arraycopy(contents, index + 1, contents, index, size - index);
    }

    contents[size] = null;
    return removed;
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
  public T get(int index) {
    Objects.checkIndex(index, size);
    return contents[index];
  }

  @Override
  public boolean contains(T value) {
    return indexOf(value) != -1;
  }

  @Override
  public int indexOf(T value) {
    if (isEmpty()) {
      return -1;
    }

    for (int i = 0; i < size; i++) {
      T containedValue = contents[i];

      if (Objects.equals(value, containedValue)) {
        return i;
      }
    }

    return -1;
  }
}
