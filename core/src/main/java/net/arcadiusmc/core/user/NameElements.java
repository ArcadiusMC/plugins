package net.arcadiusmc.core.user;

import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.user.name.NameElement;
import net.kyori.adventure.text.Component;

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

  public static NameElement combine(NameElement n1, NameElement n2) {
    return (user, context) -> {
      Component first = n1.createDisplay(user, context);

      if (Text.isEmpty(first)) {
        return n2.createDisplay(user, context);
      }

      return first;
    };
  }
}