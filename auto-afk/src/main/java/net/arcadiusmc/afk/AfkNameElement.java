package net.arcadiusmc.afk;

import lombok.RequiredArgsConstructor;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.DisplayIntent;
import net.arcadiusmc.user.name.NameElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class AfkNameElement implements NameElement {

  private final Afk afk;

  @Override
  public @Nullable Component createDisplay(User user, DisplayContext context) {
    if (!context.intentMatches(DisplayIntent.TABLIST)) {
      return null;
    }

    return afk.getState(user)
        .filter(PlayerAfkState::isAfk)
        .map(state -> Messages.renderText("afk.suffix", context.viewer()))
        .orElse(null);
  }
}
