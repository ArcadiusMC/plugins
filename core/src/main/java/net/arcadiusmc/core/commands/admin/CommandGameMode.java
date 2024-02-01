package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.permissions.Permission;

public class CommandGameMode extends BaseCommand {

  public CommandGameMode() {
    super("gm");

    setPermission(CorePermissions.CMD_GAMEMODE);
    setAliases("gamemode");
    setDescription("Sets your or another player's gamemode");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory = factory.withPrefix("<game mode>");
    factory.usage("")
        .addInfo("Sets your game mode to <game mode>");

    factory.usage("<player>")
        .addInfo("Sets a <player>'s game mode to <game mode>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("gamemode", ArgumentTypes.gameMode())
            .executes(c -> {
              User user = getUserSender(c);
              GameMode gameMode = c.getArgument("gamemode", GameMode.class);

              if (!user.hasPermission(gamemodePermission(gameMode))) {
                throw Exceptions.NO_PERMISSION;
              }

              user.setGameMode(gameMode);
              sendGameModeMessages(c.getSource(), user, gameMode);
              return 0;
            })

            .then(argument("user", Arguments.ONLINE_USER)
                .requires(s -> s.hasPermission(CorePermissions.CMD_GAMEMODE_OTHERS))

                .executes(c -> {
                  var source = c.getSource();
                  User user = Arguments.getUser(c, "user");
                  GameMode gameMode = c.getArgument("gamemode", GameMode.class);

                  user.setGameMode(gameMode);
                  sendGameModeMessages(source, user, gameMode);
                  return 0;
                })
            )
        );
  }

  static void sendGameModeMessages(CommandSource source, User target, GameMode mode) {
    Component modeName = gameModeName(mode);

    // If self, only tell sender they changed their game mode
    if (target.getName().equals(source.textName())) {
      target.sendMessage(
          Messages.MESSAGE_LIST.render("cmd.gameMode.changed.self")
              .addValue("gamemode", modeName)
              .create(target)
      );
      return;
    }

    // If the target user cannot see the admin broadcast that
    // their game mode was changed
    if (!target.hasPermission(gamemodePermission(mode))) {
      target.sendMessage(
          Messages.MESSAGE_LIST.render("cmd.gameMode.changed.target")
              .addValue("gamemode", modeName)
              .addValue("target", target)
              .addValue("sender", source)
              .create(target)
      );
    }

    source.sendSuccess(
        Messages.MESSAGE_LIST.render("cmd.gameMode.changed.other")
            .addValue("gamemode", modeName)
            .addValue("target", target)
            .addValue("sender", source)
            .create(source)
    );
  }

  static Component gameModeName(GameMode mode) {
    String messageKey = "cmd.gameMode." + mode.name().toLowerCase();
    return Messages.MESSAGE_LIST.renderText(messageKey, null);
  }

  static Permission gamemodePermission(GameMode mode) {
    return switch (mode) {
      case CREATIVE -> CorePermissions.CMD_GAMEMODE_CREATIVE;
      case ADVENTURE -> CorePermissions.CMD_GAMEMODE_ADVENTURE;
      case SPECTATOR -> CorePermissions.CMD_GAMEMODE_SPECTATOR;
      case SURVIVAL -> CorePermissions.CMD_GAMEMODE;
    };
  }
}