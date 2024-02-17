package net.arcadiusmc.signshops;

import net.arcadiusmc.Permissions;
import net.arcadiusmc.utils.TieredPermission;
import net.arcadiusmc.utils.TieredPermission.TierPriority;
import org.bukkit.permissions.Permission;

public interface SPermissions {
  Permission SIGNSHOPS = Permissions.register("arcadius.signshops");
  Permission EDIT = Permissions.register(SIGNSHOPS, "edit");
  Permission ADMIN = Permissions.register(SIGNSHOPS, "admin");

  TieredPermission SHOP_SIZE = TieredPermission.builder()
      .prefix("arcadius.signshops.invsize.")
      .priority(TierPriority.HIGHEST)
      .tiers(9, 18, 27, 36, 45, 54)
      .build();
}
