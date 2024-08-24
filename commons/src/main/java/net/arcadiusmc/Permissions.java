package net.arcadiusmc;

import net.arcadiusmc.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

public final class Permissions {
  private Permissions() {}

  public static final Permission VANISH         = register("arcadius.vanish");
  public static final Permission VANISH_SEE     = register(VANISH, "see");
  public static final Permission VANISH_OTHERS  = register(VANISH, "others");

  public static final Permission HELP           = registerCmd("help");
  public static final Permission WORLD_BYPASS   = register("arcadius.worldbypass");

  public static final Permission PROFILE        = registerCmd("profile");
  public static final Permission PROFILE_BYPASS = register(PROFILE, "bypass");

  public static final Permission IGNORE_AC      = registerCmd("ignoreac");

  public static final Permission ADMIN_HELP_INFO = registerCmd("admininfo");

  public static final Permission OVERRIDE_SIGN_OWNERSHIP = register("arcadius.ignore-sign-owner");

  public static Permission register(String key) {
    var manager = Bukkit.getPluginManager();

    Permission permission = manager.getPermission(key);

    if (permission == null) {
      permission = new Permission(key);
      manager.addPermission(permission);
    }

    return permission;
  }

  public static Permission registerCmd(String name) {
    return register(Commands.getDefaultPermission(name));
  }

  public static Permission register(Permission prefix, String suffix) {
    return register(prefix.getName() + "." + suffix);
  }

  public static Permission register(String prefix, String suffix) {
    return register(prefix + "." + suffix);
  }
}