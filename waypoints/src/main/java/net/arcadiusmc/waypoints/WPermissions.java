package net.arcadiusmc.waypoints;

import static net.arcadiusmc.Permissions.register;

import org.bukkit.permissions.Permission;

public interface WPermissions {

  Permission WAYPOINTS               = register("arcadius.waypoints");
  Permission WAYPOINTS_ADMIN         = register(WAYPOINTS, "admin");
  Permission IGNORE_DISCOVERY        = register(WAYPOINTS, "ignorediscovered");
  Permission WAYPOINTS_FLAGS         = register(WAYPOINTS, "flags");

}