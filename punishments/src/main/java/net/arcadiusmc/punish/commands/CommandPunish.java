package net.arcadiusmc.punish.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.punish.menus.AdminUi;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

class CommandPunish extends BaseCommand {

  public CommandPunish() {
    super("punish");
    setDescription("Opens the punishment GUI");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>")
        .addInfo("Opens the Punishment GUI for the specified player");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("player", Arguments.USER)
        .executes(c -> {
          User source = getUserSender(c);
          User target = Arguments.getUser(c, "player");
          AdminUi.open(source, target);
          return 0;
        })
    );
  }
}
