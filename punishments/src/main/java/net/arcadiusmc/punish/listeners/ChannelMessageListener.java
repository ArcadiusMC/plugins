package net.arcadiusmc.punish.listeners;

import net.arcadiusmc.events.ChannelMessageEvent;
import net.arcadiusmc.punish.BannedWords;
import net.arcadiusmc.punish.MuteState;
import net.arcadiusmc.punish.PunishPlugin;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.text.channel.ChannelMessageState;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class ChannelMessageListener implements Listener {

  private final BannedWords bannedWords;

  public ChannelMessageListener(PunishPlugin plugin) {
    this.bannedWords = plugin.getBannedWords();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onChannelMessage(ChannelMessageEvent event) {
    User source = Audiences.getUser(event.getUserSource());

    if (source == null || event.isAnnouncement()) {
      return;
    }

    MuteState mute = Punishments.checkMute(source);

    if (mute == MuteState.HARD) {
      event.setState(ChannelMessageState.CANCELLED);
      return;
    }

    if (mute == MuteState.SOFT) {
      event.setState(ChannelMessageState.SOFT_CANCELLED);
    }

    if (source instanceof CommandSender cmdSource
        && bannedWords.checkAndWarn(cmdSource, event.getMessage())
    ) {
      event.setState(ChannelMessageState.CANCELLED);
    }
  }
}