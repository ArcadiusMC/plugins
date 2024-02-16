package net.arcadiusmc.punish.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class PardonCommand extends BaseCommand {

  private final PunishType punishType;

  public PardonCommand(String name, PunishType type) {
    super(name);
    this.punishType = type;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("player", Arguments.USER)
        .executes(c -> {
          CommandSource source = c.getSource();
          User target = Arguments.getUser(c, "player");

          PunishCommands.ensureCanPardon(source, target, punishType);

          PunishEntry entry = Punishments.entry(target);
          entry.pardon(punishType, source);

          return 0;
        })
    );
  }
}
