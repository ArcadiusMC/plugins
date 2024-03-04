package net.arcadiusmc.waypoints;

import java.util.List;
import net.arcadiusmc.command.settings.BookSetting;
import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.waypoints.menu.WaypointOrder;

public class WaypointPrefs {

  public static final UserProperty<Boolean> INVITES_ALLOWED
      = Properties.booleanProperty("regionInvites", true);

  public static final UserProperty<Boolean> HULK_SMASH_ENABLED
      = Properties.booleanProperty("hulkSmash", true);

  public static final UserProperty<Boolean> HULK_SMASHING
      = Properties.booleanProperty("hulkSmashing", false);

  public static final UserProperty<Boolean> MENU_ORDER_INVERTED
      = Properties.booleanProperty("waypoints/menu_order_inverted", false);

  public static final UserProperty<WaypointOrder> MENU_ORDER
      = Properties.enumProperty("waypoints/list_order", WaypointOrder.NAME);

  static void createSettings(SettingsBook<User> settingsBook) {
    Setting hulkSmashing = Setting.create(WaypointPrefs.HULK_SMASH_ENABLED)
        .setMessageKey("settings.hulkSmashing");

    Setting regionInvites = Setting.create(WaypointPrefs.INVITES_ALLOWED)
        .setMessageKey("settings.regionInvites");

    List<BookSetting<User>> list = settingsBook.getSettings();
    list.add(regionInvites.toBookSettng());
    list.add(hulkSmashing.toBookSettng());
  }
}
