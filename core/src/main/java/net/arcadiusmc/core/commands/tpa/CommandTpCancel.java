package net.arcadiusmc.core.commands.tpa;

import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandTpCancel extends BaseCommand {

  public CommandTpCancel() {
    super("tpcancel");

    setPermission(TpPermissions.TPA);
    setDescription("Cancels a teleport");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);

      if (user.isTeleporting()) {
        throw TpExceptions.NOT_CURRENTLY_TELEPORTING;
      }

      user.getCurrentTeleport().interrupt();
      return 0;
    });
  }
}