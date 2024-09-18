package net.arcadiusmc.cosmetics.command;

import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.cosmetics.CosmeticsPlugin;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.permissions.Permission;

public class CommandCosmetics extends BaseCommand {

  static final Permission PERMISSION = Permissions.register("arcadius.cosmetics");

  private final CosmeticsPlugin plugin;

  public CommandCosmetics(CosmeticsPlugin plugin) {
    super("cosmetics");

    this.plugin = plugin;

    setDescription("Cosmetics menu command");
    setPermission(PERMISSION);

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);
      plugin.getMenus().getMenu().open(user);
      return 0;
    });
  }
}
