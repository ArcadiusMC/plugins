package net.arcadiusmc.holograms;

import java.util.Optional;
import java.util.Set;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.Result;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * API for interfacing with holographic text displays that dynamically display information
 * and are rendered per-player.
 * @see LeaderboardSource
 */
public interface HologramService {

  /**
   * Get the leaderboard source registry.
   * <p>
   * This registry won't be frozen, so modification can happen at any time,
   * but it is required that all sources be registered before the {@link ServerLoadEvent}
   * is called after start up.
   *
   * @return Source registry
   * @see LeaderboardSource
   */
  Registry<LeaderboardSource> getSources();

  /**
   * Gets a leaderboard by its name.
   * @param name Leaderboard name
   * @return Leaderboard optional
   */
  Optional<Leaderboard> getLeaderboard(String name);

  /**
   * Creates a leaderboard.
   * <p>
   * This method will return an erroneous result if the {@code name} is empty or null,
   * if the name is not a valid registry key (According to {@link Registries#isValidKey(String)}),
   * or if a leaderboard with the specified {@code name} already exists.
   * <p>
   * Leaderboards and holograms can have the same names, as they are considered completely separate
   * entities
   *
   * @param name Leaderboard name
   * @return Creation result
   */
  Result<Leaderboard> createLeaderboard(String name);

  /**
   * Creates a hologram.
   * <p>
   * This method will return an erroneous result if the {@code name} is empty or null,
   * if the name is not a valid registry key (According to {@link Registries#isValidKey(String)}),
   * or if a hologram with the specified {@code name} already exists.
   * <p>
   * Leaderboards and holograms can have the same names, as they are considered completely separate
   * entities
   *
   * @param name
   * @return
   */
  Result<TextHologram> createHologram(String name);

  /**
   * Gets a hologram by its name
   * @param name Hologram name
   * @return Hologram optional
   */
  Optional<TextHologram> getHologram(String name);

  /**
   * Removes a leaderboard.
   * <p>
   * This method will also kill the leaderboard if it has been spawned.
   *
   * @param name Leaderboard name
   * @return {@code true}, if the leaderboard was found and removed, {@code false} otherwise
   */
  boolean removeLeaderboard(String name);

  /**
   * Removes a hologram.
   * <p>
   * This method will also kill the hologram if it has been spawned.
   *
   * @param name Hologram name
   * @return {@code true}, if the hologram was found and removed, {@code false} otherwise
   */
  boolean removeHologram(String name);

  Set<String> getExistingLeaderboards();

  Set<String> getExistingHolograms();

  /**
   * Forces all leaderboards with the specified source to be updated for any players
   * nearby
   *
   * @param source Leaderboard source registry entry
   */
  void updateWithSource(Holder<LeaderboardSource> source);
}
