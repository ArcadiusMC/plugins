package net.arcadiusmc.core.commands.tpa;

import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandTpDenyAll extends BaseCommand {

  public CommandTpDenyAll() {
    super("tpdenyall");

    setPermission(TpPermissions.TPA);
    setDescription("Denies all incoming tpa requests");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      User user = getUserSender(c);

      if (!TeleportRequests.clearIncoming(user)) {
        throw TpExceptions.NO_TP_REQUESTS;
      }

      user.sendMessage(TpMessages.TPA_DENIED_ALL);
      return 0;
    });
  }
}