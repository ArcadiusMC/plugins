package net.arcadiusmc.user;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface UserFlags {

  /**
   * Test if a player has a flag set.
   *
   * @param playerId Player UUID
   * @param flag Flag value
   *
   * @return {@code true}, if the player has the flag set, {@code false} otherwise
   *
   * @throws NullPointerException If either {@code flag} or {@code playerId} is {@code null}
   */
  boolean hasFlag(@NotNull UUID playerId, @NotNull String flag);

  /**
   * Set a player flag
   *
   * @param playerId Player UUID
   * @param flag Flag value
   *
   * @return {@code true} if the flag was set, {@code false} if the flag was already set.
   *
   * @throws NullPointerException If either {@code flag} or {@code playerId} is {@code null}
   */
  boolean setFlag(@NotNull UUID playerId, @NotNull String flag);

  /**
   * Unset a player flag
   *
   * @param playerId PlayerUUID
   * @param flag Flag value
   *
   * @return {@code true}, if the flag was unset, {@code false} otherwise.
   *
   * @throws NullPointerException If either {@code flag} or {@code playerId} is {@code null}
   */
  boolean unsetFlag(@NotNull UUID playerId, @NotNull String flag);

  /**
   * Get all flags set for a player.
   *
   * @param playerId Player UUID
   * @return Player flag set
   *
   * @throws NullPointerException If {@code playerId} is {@code null}
   */
  Set<String> getFlags(@NotNull UUID playerId);
}
