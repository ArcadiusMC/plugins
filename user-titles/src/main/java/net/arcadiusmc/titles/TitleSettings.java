package net.arcadiusmc.titles;

import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public class TitleSettings {

  public static final UserProperty<Boolean> SEE_RANKS = Properties.booleanProperty()
      .defaultValue(false)
      .key("rankedNameTags")
      .build();

  static void add(SettingsBook<User> settingsBook) {
    var setting = Setting.create(SEE_RANKS)
        .setMessageKey("settings.rankchat")
        .createCommand("rankchat", "toggle-rankchat");

    settingsBook.getSettings().add(setting.toBookSettng());
  }
}
