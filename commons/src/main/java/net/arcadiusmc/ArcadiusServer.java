package net.arcadiusmc;

import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.MessageRenderer;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for general functions
 */
public interface ArcadiusServer {

  /**
   * Get the server instance
   * @return Server instance
   */
  static ArcadiusServer server() {
    return ServiceInstances.getServer();
  }

  /**
   * Get the current server spawn.
   * <p>
   * If the server spawn is not explicitly set, then this will return the spawn
   * location of the overworld.
   *
   * @return Server spawn
   */
  @NotNull
  Location getServerSpawn();

  /**
   * Set the current server spawn.
   * @param serverSpawn New server spawn.
   */
  void setServerSpawn(@NotNull Location serverSpawn);

  @NotNull
  SettingsBook<User> getGlobalSettingsBook();

  MessageRenderer getAnnouncementRenderer();

  void announce(ViewerAwareMessage message);

  default void announce(ComponentLike like) {
    announce(viewer -> Text.valueOf(like, viewer));
  }

  /**
   * Register a listener for the {@code /leave} command.
   *
   * @param id Listener ID
   * @param listener Listener callback
   *
   * @throws NullPointerException If either {@code id} or {@code listener} is {@code null}
   */
  void registerLeaveListener(String id, LeaveCommandListener listener);

  /**
   * Unregister a listener for the {@code /leave} command.
   *
   * @param id Listener ID
   *
   * @throws NullPointerException If {@code id} is {@code null}
   */
  void unregisterLeaveListener(String id);

  /**
   * Spawn a pile of coins that can be picked up by players.
   *
   * @param location Coin pile spawn location
   * @param value The amount of money the pile will reward.
   * @param pileSize The model size to use.
   */
  void spawnCoinPile(Location location, int value, CoinPileSize pileSize);

  /**
   * Test if a specified {@code entity} is a coin pile or not
   * @param entity Entity to test
   * @return {@code true}, if the entity is a coin pile, {@code false} otherwise.
   */
  @Contract("null -> false")
  boolean isCoinPile(@Nullable Entity entity);

  /**
   * Coin pile size
   */
  enum CoinPileSize {
    SMALL,
    MEDIUM,
    LARGE,
    ;
  }

  /**
   * {@code /leave} command callback.
   */
  interface LeaveCommandListener {

    /**
     * Called when a player runs the {@code /leave} command.
     * <p>
     * If this method returns {@code true}, the system stops iterating through
     * listeners and stops successfully.
     * <p>
     * If this method returns {@code false}, the system will move on to the next
     * listener and invoke it until a successful result is found, or until all
     * listeners have been executed.
     *
     * @param player Player that executed the command.
     * @return {@code true}, if the player was able to use the command here,
     *         {@code false} otherwise.
     */
    boolean onUse(User player);
  }
}