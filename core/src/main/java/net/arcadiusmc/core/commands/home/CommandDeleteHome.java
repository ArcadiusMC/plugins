package net.arcadiusmc.core.commands.home;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;

public class CommandDeleteHome extends BaseCommand {

  public CommandDeleteHome() {
    super("deletehome");

    setPermission(CorePermissions.HOME);
    setDescription("Deletes a home");
    setAliases("removehome", "remhome", "yeethome", "delhome");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("")
        .addInfo("Deletes your home named 'home'");

    factory.usage("<home>")
        .addInfo("Deletes <home>");

    factory.usage("<player>:<home>")
        .setPermission(CorePermissions.HOME_OTHERS)
        .addInfo("Deletes a <player>'s <home>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /deletehome
        .executes(c -> {
          User user = getUserSender(c);
          UserHomes homes = user.getComponent(UserHomes.class);

          if (!homes.contains(UserHomes.DEFAULT)) {
            throw HomeMessages.NO_DEFAULT.exception(user);
          }

          return delHome(c.getSource(), HomeParseResult.DEFAULT);
        })

        // /deletehome <home>
        .then(argument("home", HomeArgument.HOME)
            .executes(c -> {
              HomeParseResult result = c.getArgument("home", HomeParseResult.class);
              return delHome(c.getSource(), result);
            })
        );
  }

  private int delHome(CommandSource source, HomeParseResult result) throws CommandSyntaxException {
    Home h = result.get(source, true);
    String name = h.name();

    User user;

    if (result.getUser() == null) {
      user = Users.get(source.asPlayer());
    } else {
      user = Users.get(result.getUser());
    }

    UserHomes homes = user.getComponent(UserHomes.class);

    homes.remove(name);
    user.playSound(Sound.UI_TOAST_IN, 2, 1.3f);

    boolean self = source.textName().equals(user.getName());
    boolean defaultHome = Objects.equals(name, UserHomes.DEFAULT);

    MessageRef message;

    if (self) {
      message = defaultHome
          ? HomeMessages.DELETED_DEF_SELF
          : HomeMessages.DELETED_SELF;
    } else {
      message = defaultHome
          ? HomeMessages.DELETED_DEF_OTHER
          : HomeMessages.DELETED_OTHER;
    }

    Component renderedMessage = message.get()
        .addValue("home.name", name)
        .addValue("home.location", h.location())
        .addValue("player", user)
        .create(source);

    if (!self) {
      source.sendSuccess(renderedMessage);
    } else {
      source.sendMessage(renderedMessage);
    }

    return 0;
  }
}