package net.arcadiusmc.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.Objects;
import net.arcadiusmc.user.User;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WgUtils {
  private WgUtils() {}

  /**
   * State flag that can be used to block players teleporting in and out of special server areas
   */
  public static final StateFlag PLAYER_TELEPORTING = new StateFlag("player-tp-allowed", true);

  /**
   * Tests if a player has flag bypass permissions
   * @param user Player
   * @param world World context
   * @return {@code true}, if player has flag override, {@code false} otherwise.
   */
  public static boolean hasBypass(User user, World world) {
    LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(user.getPlayer());

    return WorldGuard.getInstance()
        .getPlatform()
        .getSessionManager()
        .hasBypass(wgPlayer, BukkitAdapter.adapt(world));
  }

  /**
   * Gets a World Guard flag's value at a location
   * @param location Location to query for a value
   * @param flag Flag to query the value of
   * @return Flag value
   */
  public static <T> T getFlagValue(@NotNull Location location, @NotNull Flag<T> flag) {
    return getFlagValue(location, flag, null);
  }

  /**
   * Gets a World Guard flag's value at a location and takes into consideration a player's
   * permissions
   *
   * @param location Location to query for a value
   * @param flag Flag to query the value of
   * @param player The player querying the value
   * @return Flag value
   */
  public static <T> T getFlagValue(
      @NotNull Location location,
      @NotNull Flag<T> flag,
      @Nullable Player player
  ) {
    Objects.requireNonNull(location, "Null location");
    Objects.requireNonNull(flag, "Null flag");

    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionQuery query = container.createQuery();

    LocalPlayer localPlayer;

    if (player == null) {
      localPlayer = null;
    } else {
      localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    }

    return query.queryValue(BukkitAdapter.adapt(location), localPlayer, flag);
  }

  /**
   * Tests a state flag value
   * @param location Location to query for a valueon
   * @param flag Flag to query the value of
   * @return {@code true}, if the flag is unset and the flag's default value is {@code ALLOW}, or if
   *         the flag's value is set to {@code ALLOW}. Otherwise {@code false} is returned
   */
  public static boolean testFlag(@NotNull Location location, @NotNull StateFlag flag) {
    return testFlag(location, flag, null);
  }

  /**
   * Tests a state flag value with a player association
   *
   * @param location Location to query for a valueon
   * @param flag Flag to query the value of
   * @param player Player querying the value
   *
   * @return {@code true}, if the flag is unset and the flag's default value is {@code ALLOW}, or if
   *         the flag's value is set to {@code ALLOW}. Otherwise {@code false} is returned
   */
  public static boolean testFlag(
      @NotNull Location location,
      @NotNull StateFlag flag,
      @Nullable Player player
  ) {
    State state = getFlagValue(location, flag, player);

    if (state == null) {
      return flag.getDefault() == State.ALLOW;
    }

    return state == State.ALLOW;
  }
}
