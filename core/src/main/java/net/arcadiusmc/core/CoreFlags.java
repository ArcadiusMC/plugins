package net.arcadiusmc.core;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class CoreFlags {

  public static final StateFlag TRAPDOOR_USE = new StateFlag("trapdoor-use", true);
  public static final StateFlag HEALTH_BARS = new StateFlag("health-bars", true);
  public static final StateFlag DAMAGE_INDICATORS = new StateFlag("damage-numbers", true);
  public static final StateFlag WILD_ALLOWED = new StateFlag("wild-allowed", false);

  static void registerAll() {
    FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
    registry.register(TRAPDOOR_USE);
    registry.register(WILD_ALLOWED);
    registry.register(HEALTH_BARS);
    registry.register(DAMAGE_INDICATORS);
  }
}
