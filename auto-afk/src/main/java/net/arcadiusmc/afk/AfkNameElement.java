package net.arcadiusmc.afk;

import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.DisplayIntent;
import net.arcadiusmc.user.name.NameElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class AfkNameElement implements NameElement {

  @Override
  public @Nullable Component createDisplay(User user, DisplayContext context) {
    if (!Afk.isAfk(user)) {
      return null;
    }
    if (!context.intentMatches(DisplayIntent.TABLIST)) {
      return null;
    }

    return Messages.renderText("afk.suffix", context.viewer());
  }
}
