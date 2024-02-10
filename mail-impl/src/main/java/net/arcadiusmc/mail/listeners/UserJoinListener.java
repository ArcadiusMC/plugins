package net.arcadiusmc.mail.listeners;

import net.arcadiusmc.mail.MailService;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.event.UserJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    user.sendMessage(
        Component.text()
            .append(Component.text("You have mail!", NamedTextColor.YELLOW))
            .append(
                Component.text(" [Click to view mail]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/mail"))
                    .hoverEvent(Messages.CLICK_ME)
            )
            .build()
    );
  }
}
