package net.arcadiusmc.mail.listeners;

import net.arcadiusmc.events.Events;
import net.arcadiusmc.mail.MailService;

public class MailListeners {

  public static void registerAll(MailService service) {
    Events.register(new UserJoinListener(service));
  }
}
