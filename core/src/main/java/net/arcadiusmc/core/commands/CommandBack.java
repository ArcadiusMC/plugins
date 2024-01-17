package net.arcadiusmc.core.commands;

import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.events.WorldAccessTestEvent;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
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
            throw CoreExceptions.NO_RETURN;
          }

          WorldAccessTestEvent.testWorldAccess(user.getPlayer(), ret.getWorld()).orThrow(() -> {
            return Text.format("Not allowed to return to world {0}",
                Text.formatWorldName(ret.getWorld())
            );
          });

          user.createTeleport(user::getReturnLocation, UserTeleport.Type.BACK)
              .start();

          return 0;
        });
  }
}