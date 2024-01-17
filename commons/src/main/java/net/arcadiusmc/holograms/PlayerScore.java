package net.arcadiusmc.holograms;

import java.util.UUID;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.DisplayIntent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public record PlayerScore(UUID playerId, int value) implements LeaderboardScore {

  @Override
  public @NotNull Component displayName(Audience viewer) {
    return Users.get(playerId).displayName(viewer, DisplayIntent.HOLOGRAM);
  }
}
