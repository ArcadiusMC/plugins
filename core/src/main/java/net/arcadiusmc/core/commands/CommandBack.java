package net.arcadiusmc.core.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import net.arcadiusmc.utils.WgUtils;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Location;

public class CommandBack extends BaseCommand {

  public CommandBack() {
    super("back");

    setPermission(CorePermissions.BACK);
    setAliases("return");
    setDescription("Teleports you to your previous location");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          if (!user.checkTeleporting()) {
            return 0;
          }

          Location ret = user.getReturnLocation();
          if (ret == null) {
            throw CoreExceptions.NO_RETURN.exception(user);
          }

          if (!WgUtils.testFlag(ret, WgUtils.PLAYER_TELEPORTING)) {
            throw CoreExceptions.RETURN_FORBIDDEN.exception(user);
          }
          if (!WgUtils.testFlag(user.getLocation(), WgUtils.PLAYER_TELEPORTING, user.getPlayer())) {
            throw Messages.tpNotAllowedHere(user);
          }

          user.createTeleport(user::getReturnLocation, UserTeleport.Type.BACK)
              .start();

          return 0;
        });
  }
}