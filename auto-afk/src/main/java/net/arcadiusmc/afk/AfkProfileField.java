package net.arcadiusmc.afk;

import lombok.RequiredArgsConstructor;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.ProfileDisplayElement;

@RequiredArgsConstructor
public class AfkProfileField implements ProfileDisplayElement {

  private final Afk afk;

  @Override
  public void write(TextWriter writer, User user, DisplayContext context) {
    afk.getState(user)
        .filter(PlayerAfkState::isAfk)
        .map(PlayerAfkState::getReason)
        .ifPresent(message -> {
          writer.field(
              Messages.renderText("afk.profileField", context.viewer()),
              message
          );
        });
  }
}
