package net.arcadiusmc.staffchat;

import com.google.common.base.Strings;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import java.util.Optional;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.text.PlayerMessage;

class StaffChatDiscordListener {

  private final StaffChatPlugin plugin;

  public StaffChatDiscordListener(StaffChatPlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onMessage(DiscordGuildMessageReceivedEvent event) {
    var channel = event.getChannel();
    var author = event.getAuthor();

    if (author.isBot()
        || author.isSystem()
        || event.getMessage().isWebhookMessage()
    ) {
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
        .setSource(event.getMember())
        .setLogged(false)
        .setFromDiscord(true)
        .setMessage(PlayerMessage.allFlags(event.getMessage().getContentDisplay()))
        .send();
  }
}
