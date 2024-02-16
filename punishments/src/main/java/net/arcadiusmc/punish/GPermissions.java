package net.arcadiusmc.punish;

import static net.arcadiusmc.Permissions.register;

import net.arcadiusmc.Permissions;
import org.bukkit.permissions.Permission;

public interface GPermissions {
  Permission STAFF_NOTES = Permissions.register("arcadius.staffnotes");
  Permission BROADCASTS = Permissions.register("arcadius.punish.announcements");
  Permission ADMIN = Permissions.register("arcadius.punish.admin");
  Permission EAVESDROP = register("arcadius.eavesdrop");
  Permission EAVESDROP_ADMIN = register(EAVESDROP.getName() + ".admin");
}
