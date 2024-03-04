package net.arcadiusmc.waypoints;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.WgUtils;
import org.bukkit.block.Block;

public final class WaypointWorldGuard {
  private WaypointWorldGuard() {}

  public static final StateFlag CREATE_WAYPOINTS = new StateFlag("waypoint-creation", true);

  static void registerAll() {
    FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
    registry.register(CREATE_WAYPOINTS);
  }

  public static boolean canCreateAt(Block block, User user) {
    if (user.hasPermission(WPermissions.WAYPOINTS_ADMIN)) {
      return true;
    }

    return WgUtils.testFlag(block.getLocation(), CREATE_WAYPOINTS, user.getPlayer());
  }
}
