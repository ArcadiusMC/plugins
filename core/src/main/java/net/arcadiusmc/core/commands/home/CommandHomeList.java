package net.arcadiusmc.core.commands.home;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;

public class CommandHomeList extends BaseCommand {

  public CommandHomeList() {
    super("homelist");

    setAliases("homes", "listhomes");
    setPermission(CorePermissions.HOME);
    setDescription("Lists all your homes");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Lists your homes");

    factory.usage("<user>", "Lists the <user>'s homes")
        .setPermission(CorePermissions.HOME_OTHERS);
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /homes
        .executes(c -> {
          User user = getUserSender(c);

          return listHomes(user.getComponent(UserHomes.class), c.getSource(), true);
        })

        // /homes <user>
        .then(argument("user", Arguments.USER)
            .requires(s -> s.hasPermission(CorePermissions.HOME_OTHERS))

            .executes(c -> {
              User user = Arguments.getUser(c, "user");
              boolean self = user.getName().equalsIgnoreCase(c.getSource().textName());

              return listHomes(user.getComponent(UserHomes.class), c.getSource(), self);
            })
        );
  }

  private int listHomes(UserHomes homes, CommandSource source, boolean self)
      throws CommandSyntaxException
  {
    if (homes.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    User user = homes.getUser();
    boolean unlimited = CorePermissions.MAX_HOMES.hasUnlimited(user);
    MessageRef headerRef;

    if (self) {
      headerRef = unlimited
          ? HomeMessages.LIST_HEADER_SELF_UNLIMITED
          : HomeMessages.LIST_HEADER_SELF_LIMITED;
    } else {
      headerRef = unlimited
          ? HomeMessages.LIST_HEADER_OTHER_UNLIMITED
          : HomeMessages.LIST_HEADER_OTHER_LIMITED;
    }

    String prefix = self ? "" : user.getName() + ":";
    Component joinedHomes = CoreMessages.listHomes(homes, "/home " + prefix);

    source.sendMessage(
        headerRef.get()
            .addValue("homeCount", homes.size())
            .addValue("maxHomes", homes.getMaxHomes())
            .addValue("player", user)
            .addValue("homes", joinedHomes)
            .create(source)
    );
    return 0;
  }
}