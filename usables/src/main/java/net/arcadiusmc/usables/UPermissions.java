package net.arcadiusmc.usables;

import static net.arcadiusmc.Permissions.register;

import org.bukkit.permissions.Permission;

public interface UPermissions {

  Permission USABLES = register("arcadius.usables");

  Permission ADMIN_INTERACTION = register(USABLES, "adminuse");

  Permission ENTITY = register(USABLES, "entity");
  Permission BLOCK = register(USABLES, "block");
  Permission ITEM = register(USABLES, "item");
  Permission TRIGGER = register(USABLES, "trigger");

  Permission WARP = register("arcadius.warps");
  Permission WARP_ADMIN = register(WARP, "admin");

  Permission KIT = register("arcadius.kits");
  Permission KIT_ADMIN = register(KIT, "admin");
}
