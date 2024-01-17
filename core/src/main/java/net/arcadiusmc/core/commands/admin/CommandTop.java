package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import org.bukkit.HeightMap;
import org.bukkit.Location;

public class CommandTop extends BaseCommand {

  public CommandTop() {
    super("top");

    setPermission(CorePermissions.CMD_TELEPORT);
    setDescription("Teleports you to the top block in your X and Z pos");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          Location top = user.getLocation().toHighestLocation(HeightMap.WORLD_SURFACE);

          user.createTeleport(() -> top, UserTeleport.Type.TELEPORT)
              .setDelay(null)
              .setSetReturn(false)
              .start();

          return 0;
        });
  }
}