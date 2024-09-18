package net.arcadiusmc.cosmetics;

import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.cosmetics.command.Emote;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public class CosmeticsSettings {

  public static final UserProperty<Boolean> EMOTES_ENABLED
      = Properties.booleanProperty("emotesAllowed", true);

  static void registerAll(SettingsBook<User> book) {
    Setting setting = Setting.create(EMOTES_ENABLED)
        .setMessageKey("cosmetics.emotes.setting")
        .setPermission(Emote.PERMISSION)
        .createCommand("toggleemotes", "emotetoggle");

    book.getSettings().add(setting.toBookSettng());
  }
}
