package net.arcadiusmc.punish.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.arcadiusmc.punish.BannedWords;
import net.arcadiusmc.punish.MuteState;
import net.arcadiusmc.punish.PunishPlugin;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class ChatListener implements Listener {

  private final BannedWords bannedWords;

  public ChatListener(PunishPlugin plugin) {
    this.bannedWords = plugin.getBannedWords();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onAsyncChat(AsyncChatEvent event) {
    MuteState mute = Punishments.checkMute(event.getPlayer());

    if (mute == MuteState.HARD) {
      event.setCancelled(true);
      return;
    }

    if (mute == MuteState.SOFT) {
      event.viewers().removeIf(audience -> {
        if (Audiences.equals(audience, event.getPlayer())) {
          return false;
        }

        User user = Audiences.getUser(audience);

        if (user == null) {
          return false;
        }

        MuteState viewerMute = Punishments.getMute(user);
        return viewerMute != MuteState.SOFT;
      });
    }

    if (bannedWords.checkAndWarn(event.getPlayer(), event.message())) {
      event.setCancelled(true);
    }
  }
}