package net.arcadiusmc.marriages.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.marriages.MMessages;
import net.arcadiusmc.marriages.Marriages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.Users;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onAsyncChat(AsyncChatEvent event) {
    var user = Users.get(event.getPlayer());
    var spouse = Marriages.getSpouse(user);
    boolean mchat = user.get(Marriages.MCHAT_TOGGLED);

    if (!mchat) {
      return;
    }

    event.setCancelled(true);

    if (spouse == null || !spouse.isOnline()) {
      user.sendMessage(MMessages.CANNOT_SEND_MCHAT);
      return;
    }

    String rawMessage = event.signedMessage().message();
    PlayerMessage message = PlayerMessage.of(rawMessage, user);

    Loggers.getPluginLogger().info("MChat: {} > {}", user.getName(), message.getMessage());

    Marriages.mchat(user, message);
  }
}
