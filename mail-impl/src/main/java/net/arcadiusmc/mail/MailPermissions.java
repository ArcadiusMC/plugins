package net.arcadiusmc.mail;

import net.arcadiusmc.Permissions;
import org.bukkit.permissions.Permission;

public interface MailPermissions {

  Permission MAIL = Permissions.register("arcadius.mail");
  Permission MAIL_OTHERS = Permissions.register(MAIL, "others");
  Permission MAIL_ITEMS = Permissions.register(MAIL, "items");
  Permission MAIL_ADMIN = Permissions.register(MAIL, "admin");
  Permission MAIL_ALL = Permissions.register(MAIL, "send-all");
  Permission MAIL_FLAGS = Permissions.register(MAIL, "flags");
}
