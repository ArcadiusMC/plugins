package net.arcadiusmc.core.user;

import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.name.NameElement;
import net.arcadiusmc.user.name.UserNameFactory;

public final class NameElements {
  private NameElements() {}

  public static final NameElement PREFIX_PROPERTY
      = (user, context) -> user.get(Properties.PREFIX);

  public static final NameElement SUFFIX_PROPERTY
      = (user, context) -> user.get(Properties.SUFFIX);

  public static void registerAll(UserNameFactory factory) {
    factory.addPrefix("prefix_property", 0, PREFIX_PROPERTY);
    factory.addSuffix("suffix_property", 0, SUFFIX_PROPERTY);
  }
}