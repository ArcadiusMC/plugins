package net.arcadiusmc.webmap;

import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingAccess;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;

public class HideSetting {

  public static final UserProperty<Boolean> DYNMAP_HIDE
      = Properties.booleanProperty("dynmapHide", false);

  public static final SettingAccess VISIBLE = new SettingAccess() {
    @Override
    public boolean getState(User user) {
      return WebMap.map().isPlayerVisible(user.getOfflinePlayer());
    }

    @Override
    public void setState(User user, boolean state) {
      WebMap.map().setPlayerVisible(user.getOfflinePlayer(), state);
    }
  };

  static void createSetting(SettingsBook<User> settingsBook) {
    var setting = Setting.createInverted(VISIBLE)
        .setDisplayName("Dynmap Hide")
        .setToggle("N{1} hidden on Dynmap")
        .setToggleDescription("{Enable} being hidden on Dynmap")
        .setDescription("Toggles being visible on the server's Dynmap");

    settingsBook.getSettings().add(setting.toBookSettng());
  }
}
