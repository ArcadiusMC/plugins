package net.arcadiusmc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.util.Tick;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Cooldowns {

  /** Cooldown category that is never saved to disk */
  String TRANSIENT_CATEGORY = "transient";

  /** Cooldown that never ends */
  long NO_END_COOLDOWN = -1L;

  static Cooldowns cooldowns() throws IllegalStateException {
    return ServiceInstances.getCooldown();
  }

  Set<String> getExistingCategories();

  default void cooldown(@NotNull UUID playerId, @NotNull Duration time) {
    cooldown(playerId, TRANSIENT_CATEGORY, time);
  }

  void cooldown(@NotNull UUID playerId, @NotNull String category, @NotNull Duration time);

  default void cooldown(@NotNull UUID playerId, @NotNull String category, long durationTicks) {
    cooldown(playerId, category, Tick.of(durationTicks));
  }

  default boolean remove(UUID playerId) {
    return remove(playerId, TRANSIENT_CATEGORY);
  }

  /**
   * Removes a player from a cooldown
   * @param playerId The UUID of the player to remove from cooldown
   * @param category The category to remove from
   * @return True, if the player was in the category and was removed,
   *         false otherwise
   */
  boolean remove(@NotNull UUID playerId, @NotNull String category);

  default boolean onCooldown(@NotNull UUID playerId) {
    return onCooldown(playerId, TRANSIENT_CATEGORY);
  }

  /**
   * Tests if a player is on cooldown
   *
   * @param playerId The player's UUID
   * @param category The cooldown category
   * @return True, if the UUID is NOT on cooldown, false otherwise
   */
  boolean onCooldown(@NotNull UUID playerId, @NotNull String category);

  /**
   * Gets the remaining duration of a user's cooldown
   *
   * @param playerId The UUID of the player to get the remaining cooldown for
   * @param category The category to get the cooldown of
   *
   * @return Remaining duration. Returns null, if not on cooldown, and returns
   *         a negative duration, if on a never-ending cooldown
   */
  @Nullable
  Duration getRemainingTime(UUID playerId, String category);

  boolean containsOrAdd(UUID uuid, long timeMillis);

  boolean containsOrAdd(UUID uuid, String category, long timeMillis);

  void testAndThrow(UUID uuid, long timeMillis) throws CommandSyntaxException;

  void testAndThrow(UUID uuid, String category, long timeMillis) throws CommandSyntaxException;
}