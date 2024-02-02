package net.arcadiusmc.core.commands.tpa;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandTpask extends BaseCommand {

  public CommandTpask() {
    super("tpask");

    setAliases("tpa", "tprequest", "tpr", "etpa", "etpask");
    setDescription("Asks a to teleport to a player.");
    setPermission(TpPermissions.TPA);
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>").addInfo("Asks to teleport to a <player>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("player", Arguments.ONLINE_USER)
        .executes(c -> {
          User player = getUserSender(c);
          User target = Arguments.getUser(c, "player");

          TeleportRequest.run(player, target, false);
          return 0;
        })
    );
  }
}