package net.arcadiusmc.core.commands.tpa;

import static net.arcadiusmc.Permissions.registerCmd;

import net.arcadiusmc.utils.TieredPermission;
import net.arcadiusmc.utils.TieredPermission.TierPriority;
import org.bukkit.permissions.Permission;

public final class TpPermissions {
  private TpPermissions() {}

  public static final Permission TPA = registerCmd("tpa");

  public static final TieredPermission TP_DELAY = TieredPermission.builder()
      .prefix("ftc.teleport.delay.")
      .unlimitedPerm("ftc.teleport.bypass")
      .priority(TierPriority.LOWEST)
      .allowUnlimited()
      .tiersFrom1To(5)
      .build();
}
