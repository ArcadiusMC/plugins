package net.arcadiusmc.core.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandSudo extends BaseCommand {

  public CommandSudo() {
    super("sudo");
    setDescription("Execute a command as another player");
    setAliases("run-as");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player> <command>", "Makes a player perform a command");
    factory.usage("<player> say <text>", "Makes a player say something in chat");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("player", Arguments.ONLINE_USER)
        .then(argument("input", StringArgumentType.greedyString())
            .suggests(Grenadier.suggestAllCommands())

            .executes(c -> {
              CommandSource source = c.getSource();
              User target = Arguments.getUser(c, "player");
              String input = c.getArgument("input", String.class);

              target.getPlayer().performCommand(input);

              source.sendSuccess(
                  Messages.render("cmd.sudo.command")
                      .addValue("player", target)
                      .addValue("command", input)
                      .create(source)
              );
              return 0;
            })
        )
    );
  }
}
