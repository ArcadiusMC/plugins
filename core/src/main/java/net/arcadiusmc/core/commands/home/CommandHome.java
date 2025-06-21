package net.arcadiusmc.core.commands.home;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import net.arcadiusmc.user.event.HomeCommandEvent;
import net.arcadiusmc.utils.WgUtils;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Location;

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
            throw HomeMessages.NO_DEFAULT.exception(user);
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
    Location l = home.location();

    HomeCommandEvent event = new HomeCommandEvent(user, nameSet, home.name());
    event.callEvent();

    if (event.isCancelled()) {
      return 0;
    }

    if (!WgUtils.testFlag(user.getLocation(), WgUtils.PLAYER_TELEPORTING, user.getPlayer())) {
      throw Messages.tpNotAllowedHere(source);
    }
    if (!WgUtils.testFlag(l, WgUtils.PLAYER_TELEPORTING, user.getPlayer())) {
      throw HomeMessages.FORBIDDEN.exception(user);
    }

    if (!user.checkTeleporting()) {
      return 0;
    }

    UserTeleport teleport = user.createTeleport(() -> l, UserTeleport.Type.HOME);

    MessageRender render = result.isDefaultHome()
        ? HomeMessages.TELEPORT_DEF.get()
        : HomeMessages.TELEPORT_REG.get();

    teleport.setCompleteMessage(
        render
            .addValue("home.name", home.name())
            .addValue("home.location", l)
            .create(source)
    );

    teleport.start();
    return 0;
  }
}