package net.arcadiusmc.core;

import java.util.List;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.settings.BookSetting;
import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.core.commands.tpa.TpPermissions;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public class PrefsBook {

  public static final UserProperty<Boolean> IGNORE_AUTO_BROADCASTS
      = Properties.booleanProperty("ignoringBroadcasts", false);

  public static final UserProperty<Boolean> PAY
      = Properties.booleanProperty("paying", true);

  public static final UserProperty<Boolean> DURABILITY_WARNINGS
      = Properties.booleanProperty("durabilityAlerts", true);

  public static final UserProperty<String> PLAYER_DEFINED_SUFFIX
      = Properties.stringProperty()
      .key("playerDefinedSuffix")
      .callback((user, value, oldValue) -> user.updateTabName())
      .defaultValue("")
      .build();

  public static final UserProperty<String> PLAYER_DEFINED_PREFIX
      = Properties.stringProperty()
      .key("playerDefinedPrefix")
      .callback((user, value, oldValue) -> user.updateTabName())
      .defaultValue("")
      .build();

  static void init(SettingsBook<User> settings) {
    Setting flying = Setting.create(Properties.FLYING)
        .setMessageKey("settings.fly")
        .setPermission(CorePermissions.FLY)
        .createCommand("flying", "fly");

    Setting god = Setting.create(Properties.GODMODE)
        .setMessageKey("settings.god")
        .setPermission(CorePermissions.GOD)
        .createCommand("godmode", "god");

    Setting tpa = Setting.create(Properties.TPA)
        .setMessageKey("settings.tpa")
        .setPermission(TpPermissions.TPA)
        .createCommand("tpatoggle", "toggle-tpa");

    Setting profilePrivate = Setting.create(Properties.PROFILE_PRIVATE)
        .setMessageKey("settings.profilePrivate")
        .setPermission(Permissions.PROFILE)
        .createCommand("profileprivate", "profilepublic", "toggle-profile", "profile-toggle");

    Setting ignoreac = Setting.createInverted(IGNORE_AUTO_BROADCASTS)
        .setMessageKey("settings.showAnnouncements")
        .createCommand("toggle-announcements");

    Setting paying = Setting.create(PAY)
        .setMessageKey("settings.paying")
        .setPermission(CorePermissions.PAY)
        .createCommand("toggle-pay", "paytoggle");

    Setting durabilityAlerts = Setting.create(DURABILITY_WARNINGS)
        .setMessageKey("settings.durabilityWarn")
        .createCommand("durabilitywarntoggle", "toggle-durabilitywarn");

    List<BookSetting<User>> list = settings.getSettings();
    list.add(flying.toBookSettng());
    list.add(god.toBookSettng());
    list.add(tpa.toBookSettng());
    list.add(profilePrivate.toBookSettng());
    list.add(ignoreac.toBookSettng());
    list.add(paying.toBookSettng());
    list.add(durabilityAlerts.toBookSettng());
  }
}
