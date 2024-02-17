package net.arcadiusmc.mail.listeners;

import net.arcadiusmc.mail.MailService;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.event.UserJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class UserJoinListener implements Listener {

  private final MailService service;

  public UserJoinListener(MailService service) {
    this.service = service;
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    var user = event.getUser();

    if (!service.hasUnread(user.getUniqueId())) {
      return;
    }

    user.sendMessage(Messages.renderText("mail.login", user));
  }
}
