package net.arcadiusmc.core.user;

import com.google.common.base.Strings;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.PrefsBook;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.user.name.NameElement;
import net.arcadiusmc.user.name.UserNameFactory;
import org.bukkit.permissions.Permission;

public final class NameElements {
  private NameElements() {}

  public static final NameElement PREFIX_PROPERTY
      = (user, context) -> user.get(Properties.PREFIX);

  public static final NameElement SUFFIX_PROPERTY
      = (user, context) -> user.get(Properties.SUFFIX);

  public static final NameElement PLAYER_DEFINED_SUFFIX = fromStringProperty(
      PrefsBook.PLAYER_DEFINED_SUFFIX,
      CorePermissions.CMD_TAB_SUFFIX
  );

  public static final NameElement PLAYER_DEFINED_PREFIX = fromStringProperty(
      PrefsBook.PLAYER_DEFINED_PREFIX,
      CorePermissions.CMD_TAB_PREFIX
  );

  public static void registerAll(UserNameFactory factory) {
    factory.addPrefix("prefix_property", 0, PREFIX_PROPERTY);
    factory.addPrefix("player_defined_prefix", 10, PLAYER_DEFINED_PREFIX);

    factory.addSuffix("suffix_property", 0, SUFFIX_PROPERTY);
    factory.addSuffix("player_defined_suffix", 10, PLAYER_DEFINED_SUFFIX);
  }

  private static NameElement fromStringProperty(
      UserProperty<String> property,
      Permission permission
  ) {
    return (user, context) -> {
      if (!user.hasPermission(permission)) {
        return null;
      }

      String str = user.get(property);

      if (Strings.isNullOrEmpty(str)) {
        return null;
      }

      return PlayerMessage.of(" " + str, user).create(context.viewer());
    };
  }
}