package net.arcadiusmc.antigrief.listeners;

import net.arcadiusmc.antigrief.BannedWords;
import net.arcadiusmc.antigrief.Mute;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.events.ChannelMessageEvent;
import net.arcadiusmc.text.channel.ChannelMessageState;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class ChannelMessageListener implements Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onChannelMessage(ChannelMessageEvent event) {
    Audience source = event.getUserSource();

    if (source == null || event.isAnnouncement()) {
      return;
    }

    Mute mute = Punishments.checkMute(source);

    if (mute == Mute.HARD) {
      event.setState(ChannelMessageState.CANCELLED);
      return;
    }

    if (mute == Mute.SOFT) {
      event.setState(ChannelMessageState.SOFT_CANCELLED);
    }

    if (source instanceof CommandSender cmdSource
        && BannedWords.checkAndWarn(cmdSource, event.getMessage())
    ) {
      event.setState(ChannelMessageState.CANCELLED);
    }
  }
}