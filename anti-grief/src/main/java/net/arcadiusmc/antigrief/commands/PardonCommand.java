package net.arcadiusmc.antigrief.commands;

import net.arcadiusmc.antigrief.GExceptions;
import net.arcadiusmc.antigrief.PunishEntry;
import net.arcadiusmc.antigrief.PunishType;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class PardonCommand extends FtcCommand {

  private final PunishType type;

  PardonCommand(String name, PunishType type, String... aliases) {
    super(name);
    this.type = type;

    setAliases(aliases);
    setPermission(type.getPermission());
    setDescription("Pardons a user, if they've been " + type.nameEndingED());

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>", "Pardons a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("user", Arguments.USER)
        .executes(c -> {
          User user = Arguments.getUser(c, "user");
          PunishEntry entry = Punishments.entry(user);

          if (!entry.isPunished(type)) {
            throw GExceptions.notPunished(user, type);
          }

          if (!c.getSource().hasPermission(type.getPermission())) {
            throw GExceptions.cannotPardon(type);
          }

          entry.revokePunishment(type, c.getSource().textName());
          Punishments.announcePardon(c.getSource(), user, type);

          return 0;
        })
    );
  }
}