package net.arcadiusmc.utils.property;

/**
 * An arbitrary property interface that holds an ID (Used to index into a property value in an
 * {@link IdPropertyMap} instance) and a default property value with undefined nullability.
 *
 * @param <T> Property value type
 */
public interface IdProperty<T> {

  /**
   * Returns the property's ID number
   * <p>
   * The number returned by this function must be a positive integer, as it is  used to index into
   * values array stored in a {@link IdPropertyMap} instance
   *
   * @return ID number
   */
  int getId();

  /**
   * Returns the default value of this property
   * @return Default property value
   */
  T getDefaultValue();
}
