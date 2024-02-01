package net.arcadiusmc.core.commands;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;

public class CommandNear extends BaseCommand {

  public static final int DEFAULT_DISTANCE = 200;

  public CommandNear() {
    super("near");

    setAliases("nearby");
    setPermission(CorePermissions.NEARBY);
    setDescription("Shows nearby players");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory = factory.withPermission(CorePermissions.NEARBY_ADMIN);

    factory.usage("<radius: number(1..100,000)>")
        .addInfo("Shows all players with a <radius>");

    factory.usage("<user> [<radius: number(1..100,000)>]")
        .addInfo("Shows all players near to a <user>")
        .addInfo("and within an optional [range]");
  }

  private int getDefaultDistance() {
    CorePlugin plugin = CorePlugin.plugin();
    return plugin.getFtcConfig().nearCommandDistance();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /near
        .executes(c -> {
          User user = getUserSender(c);
          return showNearby(user.getLocation(), getDefaultDistance(), c.getSource());
        })

        // /near <radius>
        .then(argument("radius", IntegerArgumentType.integer(1, 100000))
            .requires(s -> s.hasPermission(CorePermissions.NEARBY_ADMIN))

            .executes(c -> {
              User user = getUserSender(c);

              return showNearby(
                  user.getLocation(),
                  c.getArgument("radius", Integer.class),
                  c.getSource()
              );
            })
        )

        // /near <player>
        .then(argument("user", Arguments.ONLINE_USER)
            .requires(s -> s.hasPermission(CorePermissions.NEARBY_ADMIN))

            .executes(c -> {
              User user = Arguments.getUser(c, "user");
              return showNearby(user.getLocation(), getDefaultDistance(), c.getSource());
            })

            // /near <player> <radius>
            .then(argument("radius", IntegerArgumentType.integer(1, 100000))
                .requires(s -> s.hasPermission(CorePermissions.NEARBY_ADMIN))

                .executes(c -> {
                  User user = Arguments.getUser(c, "user");
                  int radius = c.getArgument("radius", Integer.class);

                  return showNearby(user.getLocation(), radius, c.getSource());
                })
            )
        );
  }

  private int showNearby(Location loc, int radius, CommandSource source)
      throws CommandSyntaxException
  {
    Collection<User> players = Collections2.transform(loc.getNearbyPlayers(radius), Users::get);

    players.removeIf(user -> {
      return user.hasPermission(CorePermissions.NEARBY_IGNORE)
          || user.getGameMode() == GameMode.SPECTATOR
          || user.getName().equalsIgnoreCase(source.textName())
          || user.get(Properties.PROFILE_PRIVATE)
          || user.get(Properties.VANISHED);
    });

    if (players.isEmpty()) {
      throw CoreExceptions.NO_NEARBY_PLAYERS.exception(source);
    }

    source.sendMessage(
        CoreMessages.NEARBY_FORMAT.get()
            .addValue("players", listPlayers(players, source, loc))
            .addValue("players.size", players.size())
            .create(source)
    );
    return 0;
  }

  static Component listPlayers(Collection<User> users, CommandSource source, Location location) {
    return TextJoiner.onComma()
        .add(
            users.stream()
                .map(user -> {
                  return CoreMessages.NEARBY_ENTRY.get()
                      .addValue("player", user)
                      .addValue("distance", user.getLocation().distance(location))
                      .create(source);
                })
        )
        .asComponent();
  }
}