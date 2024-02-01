package net.arcadiusmc.core;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.arcadiusmc.utils.WgUtils;

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
    registry.register(WgUtils.PLAYER_TELEPORTING);
  }
}
