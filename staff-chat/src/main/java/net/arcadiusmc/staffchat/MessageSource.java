package net.arcadiusmc.staffchat;

import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public interface MessageSource {

  Component displayName(Audience viewer);

  boolean isVanished();

  static MessageSource simple(String name) {
    return new MessageSource() {
      @Override
      public Component displayName(Audience viewer) {
        return Component.text(name);
      }

      @Override
      public boolean isVanished() {
        return false;
      }
    };
  }

  static MessageSource of(CommandSource source) {
    return new MessageSource() {
      @Override
      public Component displayName(Audience viewer) {
        if (source.isPlayer()) {
          return Users.get(source.asPlayerOrNull()).displayName(viewer);
        }

        return source.displayName();
      }

      @Override
      public boolean isVanished() {
        return StaffChat.isVanished(source);
      }
    };
  }

}
