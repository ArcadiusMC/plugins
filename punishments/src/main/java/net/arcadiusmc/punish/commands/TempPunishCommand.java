package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;

class TempPunishCommand extends BaseCommand {

  private final PunishType type;

  public TempPunishCommand(String name, PunishType type) {
    super(name);
    this.type = type;
    setPermission(type.getPermission());
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player> <time>");
    factory.usage("<player> <time> <reason>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("player", Arguments.USER)
            .then(argument("time", ArgumentTypes.time())
                .executes(c -> {
                  User target = Arguments.getUser(c, "player");
                  CommandSource source = c.getSource();
                  Duration time = ArgumentTypes.getDuration(c, "time");

                  punish(source, target, time, null);
                  return 0;
                })

                .then(argument("reason", StringArgumentType.greedyString())
                    .executes(c -> {
                      User target = Arguments.getUser(c, "player");
                      CommandSource source = c.getSource();
                      Duration time = ArgumentTypes.getDuration(c, "time");
                      String reason = c.getArgument("reason", String.class);

                      punish(source, target, time, reason);
                      return 0;
                    })
                )
            )
        );
  }

  private void punish(CommandSource source, User target, Duration length, String reason)
      throws CommandSyntaxException
  {
    PunishCommands.ensureCanPunish(source, target, type);
    Punishments.punish(source, target, type, reason, null, length);
  }
}
