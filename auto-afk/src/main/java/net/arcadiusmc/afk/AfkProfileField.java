package net.arcadiusmc.afk;

import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.ProfileDisplayElement;

public class AfkProfileField implements ProfileDisplayElement {

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    Afk.getAfkReason(user).ifPresent(message -> {
      writer.field("AFK", message);
    });
  }
}
