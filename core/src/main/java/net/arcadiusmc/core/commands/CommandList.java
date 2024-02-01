package net.arcadiusmc.core.commands;

import java.util.ArrayList;
import java.util.Collection;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.core.CoreExceptions;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
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
          CommandSource source = c.getSource();
          Collection<User> users = new ArrayList<>(Users.getOnline());

          // If we should hide vanished
          if (!source.hasPermission(Permissions.VANISH_SEE)) {
            users.removeIf(user -> user.get(Properties.VANISHED));
          }

          // lol
          if (users.isEmpty()) {
            throw CoreExceptions.SERVER_EMPTY.exception(source);
          }

          Component playerList = CoreMessages.listPlayers(users, source);

          source.sendMessage(
              CoreMessages.PLAYER_LIST_FORMAT.get()
                  .addValue("playerList", playerList)
                  .create(source)
          );
          return 0;
        });
  }
}