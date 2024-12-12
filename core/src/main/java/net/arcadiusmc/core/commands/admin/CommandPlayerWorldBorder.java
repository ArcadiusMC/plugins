package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.Collection;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.cli.Arg;
import org.jetbrains.annotations.Nullable;

public class CommandPlayerWorldBorder extends BaseCommand {

  public CommandPlayerWorldBorder() {
    super("playerworldborder");

    setAliases("player-worldborder", "player-world-border", "pworldborder");
    setDescription("Modifies a world border for only a specific player");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("info <player>")
        .addInfo("Shows info about a player's custom world border.");

    factory.usage("reset <players>")
        .addInfo("Resets players to have the regular world border.");

    factory.usage("center <player> <center x> <center z>")
        .addInfo("Sets players' world border to be centered at specific coordinates.");

    factory.usage("diameter <player> <diameter>")
        .addInfo("Sets players' world border to have a specific size.");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("info")
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> {
                  User target = Arguments.getUser(c, "player");
                  Player player = target.getPlayer();

                  WorldBorder worldBorder = player.getWorldBorder();
                  if (!isCustomWorldBorder(worldBorder)) {
                    throw Messages.render("cmd.pworldborder.errors.notCustom")
                        .addValue("player", target)
                        .exception(c.getSource());
                  }

                  c.getSource().sendMessage(
                      Messages.render("cmd.pworldborder.info")
                          .addValue("player", target)
                          .addValue("diameter", worldBorder.getSize())
                          .addValue("center", worldBorder.getCenter())
                          .create(c.getSource())
                  );

                  return SINGLE_SUCCESS;
                })
            )
        )

        .then(literal("reset")
            .then(argument("players", Arguments.ONLINE_USERS)
                .executes(c -> {
                  List<User> users = Arguments.getUsers(c, "players");
                  users.removeIf(user -> !isCustomWorldBorder(user.getPlayer().getWorldBorder()));

                  for (User user : users) {
                    Player player = user.getPlayer();
                    player.setWorldBorder(null);
                  }

                  c.getSource().sendSuccess(msg("reset", users).create(c.getSource()));
                  return SINGLE_SUCCESS;
                })
            )
        )

        .then(literal("center")
            .then(argument("players", Arguments.ONLINE_USERS)
                .then(argument("center", ArgumentTypes.blockPosition2d())
                    .executes(c -> {
                      List<User> users = Arguments.getUsers(c, "players");
                      Location loc = ArgumentTypes.getLocation(c, "center");

                      for (User user : users) {
                        WorldBorder border = getCustomWorldBorder(user);
                        border.setCenter(loc);
                      }

                      c.getSource().sendSuccess(
                          msg("center", users)
                              .addValue("center", loc)
                              .create(c.getSource())
                      );

                      return SINGLE_SUCCESS;
                    })
                )
            )
        )

        .then(literal("diameter")
            .then(argument("players", Arguments.ONLINE_USERS)
                .then(argument("diameter", DoubleArgumentType.doubleArg())
                    .executes(c -> {
                      double dia = c.getArgument("diameter", Double.class);
                      List<User> users = Arguments.getUsers(c, "players");

                      for (User user : users) {
                        WorldBorder border = getCustomWorldBorder(user);
                        border.setSize(dia);
                      }

                      c.getSource().sendSuccess(
                          msg("diameter", users)
                              .addValue("diameter", dia)
                              .create(c.getSource())
                      );

                      return SINGLE_SUCCESS;
                    })
                )
            )
        );
  }

  private MessageRender msg(String p, Collection<User> users) {
    if (users.size() == 1) {
      return Messages.render("cmd.pworldborder", p, "single")
          .addValue("player", users.iterator().next());
    }

    return Messages.render("cmd.pworldborder", p, "multi")
        .addValue("players", users.size());
  }

  private WorldBorder getCustomWorldBorder(User user) {
    Player player = user.getPlayer();
    WorldBorder border = player.getWorldBorder();

    if (isCustomWorldBorder(border)) {
      return border;
    }

    WorldBorder custom = Bukkit.createWorldBorder();
    player.setWorldBorder(custom);

    return custom;
  }

  private boolean isCustomWorldBorder(@Nullable WorldBorder border) {
    return border != null && border.getWorld() == null;
  }
}
