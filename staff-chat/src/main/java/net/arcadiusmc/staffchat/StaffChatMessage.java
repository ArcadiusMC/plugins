package net.arcadiusmc.staffchat;

import com.google.common.base.Strings;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.utils.MarkdownSanitizer;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.text.DefaultTextWriter;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

@Getter
@Setter
@Accessors(chain = true)
public class StaffChatMessage {

  private MessageSource source;

  private ViewerAwareMessage message;
  private Component prefix;

  private boolean logged;
  private boolean discordForwarded = true;
  private boolean fromDiscord;

  public StaffChatMessage setSource(MessageSource source) {
    this.source = source;
    return this;
  }

  public StaffChatMessage setSource(Member source) {
    return setSource(MessageSource.of(source));
  }

  public StaffChatMessage setSource(CommandSource source) {
    return setSource(MessageSource.of(source));
  }

  public StaffChatMessage setSource(String sourceName) {
    return setSource(MessageSource.simple(sourceName));
  }

  public void send() {
    Objects.requireNonNull(message, "Message not specified");

    ChannelledMessage msg = ChannelledMessage.create(viewer -> {
      DefaultTextWriter writer = TextWriters.newWriter();
      writer.viewer(viewer);
      write(writer);
      return writer.asComponent();
    });

    Bukkit.getOnlinePlayers().forEach(player -> {
      if (!player.hasPermission(StaffChat.PERMISSION)) {
        return;
      }

      msg.addTarget(player);
    });

    if (logged) {
      msg.addTarget(Bukkit.getConsoleSender());
    }

    msg.send();

    if (discordForwarded && !fromDiscord) {
      sendDiscord();
    }
  }

  private void sendDiscord() {
    String channelName = StaffChatPlugin.plugin().getScConfig().getDiscordChannel();

    if (Strings.isNullOrEmpty(channelName)) {
      return;
    }

    DiscordHook.findChannel(channelName).ifPresent(channel -> {
      String strMessage = Text.toDiscord(message.asComponent());

      channel.sendMessageFormat("**%s >** %s",
          source == null
              ? "UNKNOWN"
              : MarkdownSanitizer.escape(Text.plain(source.displayName(null))),
          strMessage
      ).submit();
    });
  }

  public void write(TextWriter writer) {
    TextJoiner joiner = TextJoiner.onSpace();

    if (fromDiscord) {
      joiner.add(Messages.renderText("staffchat.prefix.discord", writer.viewer()));
    }

    if (source != null) {
      if (source.isVanished()) {
        joiner.add(Messages.renderText("staffchat.prefix.vanished", writer.viewer()));
      }

      joiner.add(source.displayName(writer.viewer()));
    }

    writer.write(
        Messages.render("staffchat.format")
            .addValue("source", joiner.asComponent())
            .addValue("message", message)
            .create(writer.viewer())
    );
  }
}
