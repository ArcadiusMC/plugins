package net.arcadiusmc.discord.listener;

import static net.kyori.adventure.text.Component.text;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.discord.Config;
import net.arcadiusmc.discord.DiscordPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class AnnouncementForwardingListener {

  private final DiscordPlugin plugin;

  public AnnouncementForwardingListener(DiscordPlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onDiscordAnnounce(DiscordGuildMessageReceivedEvent event) {
    Config config = plugin.getPluginConfig();

    boolean rule = config.forwardDiscordAnnouncementsToServer();
    long id = config.updateChannelId();

    if (id == 0 || !rule) {
      return;
    }

    if (event.getChannel() == null
        || event.getChannel().getIdLong() != id
        || event.getAuthor().isBot()
        || event.getAuthor().isSystem()
    ) {
      return;
    }

    Message msg = event.getMessage();
    String jumpTo = msg.getJumpUrl();

    Component text = Text.format("New announcement in discord! {0}",
        NamedTextColor.YELLOW,

        text("[Click here to view]", NamedTextColor.AQUA)
            .clickEvent(ClickEvent.openUrl(jumpTo))
            .hoverEvent(Messages.CLICK_ME.renderText(null))
    );

    ChannelledMessage.create(text)
        .setBroadcast()
        .setRenderer(ArcadiusServer.server().getAnnouncementRenderer())
        .send();
  }
}