package net.arcadiusmc.waypoints;

import static net.arcadiusmc.Permissions.register;
import static net.arcadiusmc.Permissions.register;

import net.arcadiusmc.Permissions;
import org.bukkit.permissions.Permission;

public interface WPermissions {

  Permission
      WAYPOINTS               = register("ftc.waypoints"),
      WAYPOINTS_ADMIN         = Permissions.register(WAYPOINTS, "admin"),
      WAYPOINTS_FLAGS         = Permissions.register(WAYPOINTS, "flags");

}