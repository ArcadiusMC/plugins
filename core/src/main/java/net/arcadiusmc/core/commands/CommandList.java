package net.arcadiusmc.core.commands;

import java.util.ArrayList;
import java.util.Collection;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;

public class CommandList extends BaseCommand {

  public CommandList() {
    super("flist");

    setAliases("list", "elist", "playerlist");
    setPermission(CorePermissions.CMD_LIST);
    setDescription("Lists all players on the server");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          Collection<User> users = new ArrayList<>(Users.getOnline());

          // If we should hide vanished
          if (!c.getSource().hasPermission(Permissions.VANISH_SEE)) {
            users.removeIf(user -> user.get(Properties.VANISHED));
          }

          // lol
          if (users.isEmpty()) {
            throw Exceptions.create("Server empty :\\");
          }

          c.getSource().sendMessage(
              CoreMessages.listHeader(users.size())
                  .append(Component.newline())
                  .append(CoreMessages.listPlayers(users, c.getSource()))
          );
          return 0;
        });
  }
}