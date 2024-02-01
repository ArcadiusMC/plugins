package net.arcadiusmc.core.commands.admin;

import static net.arcadiusmc.McConstants.TICKS_PER_DAY;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.entity.Player;

public class CommandPlayerTime extends BaseCommand {

  public CommandPlayerTime() {
    super("playertime");

    setAliases("ptime");
    setDescription("Changes the game time for a player");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              CommandSource source = c.getSource();
              Player player = get(c);

              long time = player.getPlayerTime();
              long days = time / 1000 / 24;

              source.sendMessage(
                  Messages.render("cmd.ptime.get")
                      .addValue("player", player)
                      .addValue("time.days", days)
                      .addValue("time", time)
                      .create(source)
              );
              return 0;
            })

            .then(literal("reset")
                .executes(c -> {
                  CommandSource source = c.getSource();
                  Player player = get(c);

                  player.resetPlayerTime();

                  source.sendSuccess(
                      Messages.render("cmd.ptime.reset")
                          .addValue("player", player)
                          .create(source)
                  );
                  return 0;
                })
            )

            .then(literal("set")
                .then(timeArg("day", 1000))
                .then(timeArg("noon", 6000))
                .then(timeArg("night", 13000))
                .then(timeArg("midnight", 18000))

                .then(argument("time", Arguments.GAMETIME)
                    .executes(c -> timeThing(c, false))
                )
            )

            .then(literal("add")
                .then(argument("time", Arguments.GAMETIME)
                    .executes(c -> timeThing(c, true))
                )
            )
        );
  }

  LiteralArgumentBuilder<CommandSource> timeArg(String name, long multiplier) {
    return literal(name)
        .executes(c -> {
          Player player = get(c);
          long worldTime = player.getWorld().getFullTime();
          long timeAdd = worldTime - (worldTime % TICKS_PER_DAY);
          long time = timeAdd + multiplier;

          return setTime(c.getSource(), player, time);
        });
  }

  int timeThing(CommandContext<CommandSource> c, boolean add) throws CommandSyntaxException {
    int time = c.getArgument("time", Integer.class);
    Player player = get(c);
    long actualTime = time + (add ? player.getPlayerTime() : 0);
    return setTime(c.getSource(), player, actualTime);
  }

  int setTime(CommandSource source, Player player, long time) {
    player.setPlayerTime(time, false);

    source.sendSuccess(
        Messages.render("cmd.ptime.set")
            .addValue("player", player)
            .addValue("time", time)
            .create(source)
    );
    return 0;
  }

  Player get(CommandContext<CommandSource> c) throws CommandSyntaxException {
    return Arguments.getUser(c, "user").getPlayer();
  }
}