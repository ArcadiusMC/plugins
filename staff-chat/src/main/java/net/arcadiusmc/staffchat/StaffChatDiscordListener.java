package net.arcadiusmc.staffchat;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Either;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

class StaffChatDiscordListener {

  private final StaffChatPlugin plugin;

  public StaffChatDiscordListener(StaffChatPlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onMessage(DiscordGuildMessageReceivedEvent event) {
    var channel = event.getChannel();
    var author = event.getAuthor();

    if (author.isBot() || author.isSystem() || event.getMessage().isWebhookMessage()) {
      return;
    }

    String channelName = plugin.getScConfig().getDiscordChannel();
    if (Strings.isNullOrEmpty(channelName)) {
      return;
    }

    Optional<TextChannel> coolClub = DiscordHook.findChannel(channelName);

    if (coolClub.isEmpty() || !coolClub.get().equals(channel)) {
      return;
    }

    StaffChat.newMessage()
        .setSource(wrapMember(event.getMember()))
        .setLogged(false)
        .setFromDiscord(true)
        .setMessage(PlayerMessage.allFlags(event.getMessage().getContentDisplay()))
        .send();
  }

  static MessageSource wrapMember(Member member) {
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
      public boolean isVanished() {
        return asUser().map(
            user -> user.get(Properties.VANISHED),
            member1 -> false
        );
      }
    };
  }
}
