package net.arcadiusmc.staffchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;

public class ChatListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onChatEvent(AsyncChatEvent event) {
    if (!StaffChat.toggledPlayers.contains(event.getPlayer().getUniqueId())) {
      return;
    }

    event.setCancelled(true);
    User user = Users.get(event.getPlayer());

    StaffChat.newMessage()
        .setLogged(true)
        .setMessage(PlayerMessage.allFlags(event.signedMessage().message()))
        .setSource(user.getCommandSource())
        .send();
  }
}