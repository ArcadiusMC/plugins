package net.arcadiusmc.core;

import java.util.List;
import net.arcadiusmc.command.settings.BookSetting;
import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
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

  static void init(SettingsBook<User> settings) {
    Setting flying = Setting.create(Properties.FLYING)
        .setDisplayName("Flying")
        .setDescription("Toggles being able to fly")
        .setToggle("{0} flying")
        .setToggleDescription("{Enable} flying");

    Setting god = Setting.create(Properties.GODMODE)
        .setDisplayName("Godmode")
        .setDescription("Toggles godmode")
        .setToggle("{0} God mode")
        .setToggleDescription("{Enable} God mode");

    Setting tpa = Setting.create(Properties.TPA)
        .setDisplayName("TPA")
        .setDescription("Toggles being able to send and receive TPA requests")
        .setToggle("{0} TPA requests")
        .setToggleDescription("{Enable} TPA requests");

    Setting profilePrivate = Setting.create(Properties.PROFILE_PRIVATE)
        .setDisplayName("PrivateProfile")
        .setDescription("Toggles other players being able to see your /profile")
        .setToggle("Others can n{2} see your /profile")
        .setEnableDescription("Set your profile to private")
        .setDisableDescription("Set your profile to public");

    Setting ignoreac = Setting.createInverted(IGNORE_AUTO_BROADCASTS)
        .setDisplayName("Broadcasts")
        .setDescription("Toggles seeing automatic announcements")
        .setToggle("N{1} ignoring auto announcements")
        .setEnableDescription("See auto-announcer")
        .setDisableDescription("Hide auto-announcer");

    Setting paying = Setting.create(PAY)
        .setDisplayName("Paying")
        .setDescription("Disables/Enables being able to send and receive rhines from other players")
        .setToggle("Can n{1} send or receive payments")
        .setToggleDescription("{Enable} sending and receiving rhines");

    Setting durabilityAlerts = Setting.create(DURABILITY_WARNINGS)
        .setDisplayName("Durability Warn")
        .setDescription("Toggles seeing item breaking warnings")
        .setToggle("N{1} showing item durability warnings.")
        .setToggleDescription("{Enable} item durability warnings");

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
