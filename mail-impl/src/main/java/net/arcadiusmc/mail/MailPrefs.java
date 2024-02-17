package net.arcadiusmc.mail;

import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public final class MailPrefs {
  private MailPrefs() {}

  public static final UserProperty<Boolean> MAIL_TO_DISCORD
      = Properties.booleanProperty("mailToDiscord", false);

  static void init(SettingsBook<User> book) {
    var setting = Setting.create(MAIL_TO_DISCORD)
        .setMessageKey("settings.discordMail");

    book.getSettings().add(setting.toBookSettng());
  }
}
