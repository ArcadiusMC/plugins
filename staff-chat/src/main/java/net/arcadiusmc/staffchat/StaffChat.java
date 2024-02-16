package net.arcadiusmc.staffchat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.settings.Setting;
import net.arcadiusmc.command.settings.SettingAccess;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import org.bukkit.permissions.Permission;

public final class StaffChat {
  private StaffChat() {}

  public static final Set<UUID> toggledPlayers = new HashSet<>();

  public static final Permission PERMISSION = Permissions.register("arcadius.staffchat");

  static void createSettings(SettingsBook<User> settingsBook) {
    SettingAccess access = new SettingAccess() {
      @Override
      public boolean getState(User user) {
        return toggledPlayers.contains(user.getUniqueId());
      }

      @Override
      public void setState(User user, boolean state) {
        if (state) {
          toggledPlayers.add(user.getUniqueId());
        } else {
          toggledPlayers.remove(user.getUniqueId());
        }
      }
    };

    Setting setting = Setting.create(access)
        .setMessageKey("settings.staffChatToggle")
        .setPermission(PERMISSION);

    settingsBook.getSettings().add(setting.toBookSettng());
  }

  public static boolean isVanished(CommandSource source) {
    return source != null
        && source.isPlayer()
        && Users.get(source.asPlayerOrNull()).get(Properties.VANISHED);
  }

  public static StaffChatMessage newMessage() {
    return new StaffChatMessage();
  }
}