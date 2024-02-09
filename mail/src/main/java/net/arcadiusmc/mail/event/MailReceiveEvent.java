package net.arcadiusmc.mail.event;

import lombok.Getter;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MailReceiveEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Mail message;

  public MailReceiveEvent(User user, Mail message) {
    super(user, !Bukkit.isPrimaryThread());
    this.message = message;
  }

  public Mail getMessage() {
    return message;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
