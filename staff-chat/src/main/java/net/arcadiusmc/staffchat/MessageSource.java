package net.arcadiusmc.staffchat;

import com.mojang.datafixers.util.Either;
import github.scarsz.discordsrv.dependencies.jda.api.entities.IMentionable;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import java.util.UUID;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public interface MessageSource {

  Component displayName(Audience viewer);

  boolean isVanished();

  String mentionString();

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

      @Override
      public String mentionString() {
        return null;
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

      @Override
      public String mentionString() {
        if (!source.isPlayer()) {
          return null;
        }

        return DiscordHook.getUserMember(source.asPlayerOrNull().getUniqueId())
            .map(IMentionable::getAsMention)
            .orElse(null);
      }
    };
  }

  static MessageSource of(Member member) {
    return new MessageSource() {

      Either<User, Member> asUser() {
        UUID playerId = DiscordHook.getPlayerId(member);

        if (playerId == null) {
          return Either.right(member);
        }

        var user = Users.get(playerId);
        return Either.left(user);
      }

      @Override
      public Component displayName(Audience viewer) {
        return asUser().map(
            user -> user.displayName(viewer),
            member1 -> Component.text(member1.getEffectiveName())
        );
      }

      @Override
      public String mentionString() {
        return member.getAsMention();
      }

      @Override
      public boolean isVanished() {
        return asUser().map(
            user -> user.get(Properties.VANISHED),
            member1 -> false
        );
      }
    };
  }
}
