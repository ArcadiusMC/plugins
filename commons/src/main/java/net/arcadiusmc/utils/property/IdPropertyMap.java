package net.arcadiusmc.utils.property;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.Objects;
import net.arcadiusmc.utils.ArrayIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple map of properties accessed wish {@link IdProperty} instances.
 * <p>
 * This object does not check if all {@link IdProperty} instances are the same or come from the same
 * family, nor does it enforce any rules related to overlapping ID numbers.
 * <p>
 * ID numbers must be positive integers as they're used as indices to a value array
 */
public class IdPropertyMap implements Iterable<Object> {

  private Object[] values;

  /**
   * Gets the raw values array this map stores values inside.
   * @return Map values
   */
  public Object[] getValues() {
    return values;
  }

  /**
   * Clears the map.
   * <p>
   * does not alter the underlying values array (if present) simply fills it with null values.
   */
  public void clear() {
    if (values == null) {
      return;
    }

    Arrays.fill(values, null);
  }

  /**
   * Counts the amount of non-null values stored in this map
   * @return Property value count
   */
  public int size() {
    if (values == null) {
      return 0;
    }

    int count = 0;

    for (Object value : values) {
      if (value == null) {
        continue;
      }

      count++;
    }

    return count;
  }

  /**
   * Tests if there is at least 1 non-null values stored in this map
   * @return {@code true} if there's at least 1 non-null value in the map, {@code false} otherwise
   */
  public boolean isEmpty() {
    if (values == null) {
      return true;
    }

    for (Object value : values) {
      if (value == null) {
        continue;
      }

      return false;
    }

    return true;
  }

  /**
   * Tests if this map contains a value mapped to the specified {@code property}
   * @param property Property
   * @return {@code true}, if there is a non-null value mapped to the specified property that is
   *          also NOT equal to the {@code property}'s default value. {@code false} otherwise
   */
  public <T> boolean has(@NotNull IdProperty<T> property) {
    Objects.requireNonNull(property, "Null property");

    if (values == null || values.length <= property.getId()) {
      return false;
    }

    Object value = values[property.getId()];
    Object defaultValue = property.getDefaultValue();

    if (value == null) {
      return false;
    }

    return !Objects.equals(value, defaultValue);
  }

  /**
   * Gets a value mapped to a property
   * @param property Property
   * @return Property's value, or the specified property's {@link IdProperty#getDefaultValue()}
   *         if no mapping was found.
   */
  public <T> T get(@NotNull IdProperty<T> property) {
    Objects.requireNonNull(property, "Null property");

    if (!has(property)) {
      return property.getDefaultValue();
    }

    return (T) values[property.getId()];
  }

  /**
   * Sets the value mapped to a property.
   * <p>
   * If a property's ID is larger than the current values array, this method will attempt to resize
   * the underlying values array to insert the property. Otherwise, no resizing of the array occurs
   * as a result of this method.
   *
   * @param property Property to set the value of
   * @param value Property value
   *
   * @return {@code true}, if calling this method caused any kind of change to
   *         the underlying values array, {@code false} otherwise.
   */
  public <T> boolean set(@NotNull IdProperty<T> property, @Nullable T value) {
    Objects.requireNonNull(property, "Null property");

    T current = get(property);

    // If current value == given value, don't change anything
    if ((current == null && value == null) || Objects.equals(current, value)) {
      return false;
    }

    int index = property.getId();

    // If we should unset the value
    if (value == null || Objects.equals(value, property.getDefaultValue())) {
      // If the property value array isn't large enough
      // for the property we want to unset
      if (values == null || index >= values.length) {
        return false;
      }

      values[index] = null;
    } else {
      if (values == null) {
        values = new Object[index + 1];
      } else {
        values = ObjectArrays.ensureCapacity(values, index + 1);
      }

      values[index] = value;
    }

    return true;
  }

  /**
   * Returns an iterator that iterates over all non-null values stored in this map
   * @return Value iterator
   */
  @NotNull
  @Override
  public ArrayIterator<Object> iterator() {
    return ArrayIterator.unmodifiable(values);
  }
}
