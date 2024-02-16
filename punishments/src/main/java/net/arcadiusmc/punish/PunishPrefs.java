package net.arcadiusmc.punish;

import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public final class PunishPrefs {
  private PunishPrefs() {}

  public static final UserProperty<Boolean> VIEWS_NOTES
      = Properties.booleanProperty("show_staff_notes", false);

  static void createSettings(SettingsBook<User> settingsBook) {
    Setting setting = Setting.create(VIEWS_NOTES)
        .setMessageKey("settings.showStaffNotes")
        .setPermission(GPermissions.STAFF_NOTES);

    settingsBook.getSettings().add(setting.toBookSettng());
  }
}
