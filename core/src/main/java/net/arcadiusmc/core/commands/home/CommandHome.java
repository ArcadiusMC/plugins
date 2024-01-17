package net.arcadiusmc.core.commands.home;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.events.WorldAccessTestEvent;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import net.arcadiusmc.user.event.HomeCommandEvent;

public class CommandHome extends BaseCommand {

  public CommandHome() {
    super(UserHomes.DEFAULT);

    setPermission(CorePermissions.HOME);
    setDescription("Takes you to one of your homes");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("")
        .addInfo("Teleports you to your home named 'home'");

    factory.usage("<home>")
        .addInfo("Teleports you to <home>");

    factory.usage("<player>:<home>")
        .setPermission(CorePermissions.HOME_OTHERS)
        .addInfo("Teleports to the <player>'s <home>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // No home name given -> use default home
        // /home
        .executes(c -> {
          User user = getUserSender(c);
          UserHomes homes = user.getComponent(UserHomes.class);

          //Check if they have default home
          if (!homes.contains(UserHomes.DEFAULT)) {
            throw CoreExceptions.NO_DEF_HOME;
          }

          return goHome(c.getSource(), false, user, HomeParseResult.DEFAULT);
        })

        // Home name given
        // /home <name>
        .then(argument("home", HomeArgument.HOME)
            .executes(c -> {
              User user = getUserSender(c);
              HomeParseResult result = c.getArgument("home", HomeParseResult.class);
              return goHome(c.getSource(), true, user, result);
            })
        );
  }

  private int goHome(CommandSource source, boolean nameSet, User user, HomeParseResult result)
      throws CommandSyntaxException
  {
    Home home = result.get(source, true);
    var l = home.location();

    HomeCommandEvent event = new HomeCommandEvent(user, nameSet, home.name());
    event.callEvent();

    if (event.isCancelled()) {
      return 0;
    }

    WorldAccessTestEvent.testOrThrow(
        source.asPlayer(),
        l.getWorld(),
        CoreExceptions.badWorldHome(home.name())
    );

    if (!user.checkTeleporting()) {
      return 0;
    }

    var teleport = user.createTeleport(() -> l, UserTeleport.Type.HOME);

    if (result.isDefaultHome()) {
      teleport.setCompleteMessage(CoreMessages.TELEPORTING_HOME);
    } else {
      teleport.setCompleteMessage(CoreMessages.teleportingHome(result.getName()));
    }

    teleport.start();
    return 0;
  }
}