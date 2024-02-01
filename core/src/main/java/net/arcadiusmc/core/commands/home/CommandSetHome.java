package net.arcadiusmc.core.commands.home;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.WgUtils;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Location;
import org.bukkit.Sound;

public class CommandSetHome extends BaseCommand {

  public CommandSetHome() {
    super("sethome");

    setPermission(CorePermissions.HOME);
    setDescription("Sets a home where you're standing");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("")
        .addInfo("Sets your default home, named 'home'");

    factory.usage("<home name>")
        .addInfo("Sets a home to where you're standing");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /sethome
        .executes(c -> {
          return attemptHomeSetting(getUserSender(c), UserHomes.DEFAULT);
        })

        // /sethome <home>
        .then(argument("name", StringArgumentType.word())
            .executes(c -> attemptHomeSetting(
                getUserSender(c),
                c.getArgument("name", String.class)
            ))
        );
  }


  private int attemptHomeSetting(User user, String name)
      throws CommandSyntaxException
  {
    UserHomes homes = user.getComponent(UserHomes.class);
    Location location = user.getLocation();

    boolean contains = homes.contains(name);

    if (!contains && !homes.canMakeMore()) {
      throw HomeMessages.LIMIT_REACHED.get()
          .addValue("maxHomes", homes.getMaxHomes())
          .addValue("homeCount", homes.size())
          .exception(user);
    }

    // Test to make sure the user is allowed to make
    // a home in this world.
    if (!WgUtils.testFlag(location, WgUtils.PLAYER_TELEPORTING, user.getPlayer())) {
      throw HomeMessages.SET_FORBIDDEN.exception(user);
    }

    homes.set(name, location);

    if (name.equals(UserHomes.DEFAULT)) {
      user.getPlayer().setBedSpawnLocation(location, true);
      user.sendMessage(HomeMessages.SET_DEFAULT.renderText(user));
    } else {
      user.sendMessage(
          HomeMessages.SET.get()
              .addValue("home.name", name)
              .addValue("home.location", location)
              .create(user)
      );
    }

    user.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    return 0;
  }
}