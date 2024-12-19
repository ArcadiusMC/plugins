package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;

public class CommandRunMulti extends BaseCommand {

  public CommandRunMulti() {
    super("run-multiple");

    setAliases("run-mutli", "runmutli");
    setDescription("Runs multiple commands, commands are quoted and separated by commas");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("cmd-list", ArgumentTypes.array(StringArgumentType.string()))
        .executes(c -> {
          List<String> commands = c.getArgument("cmd-list", List.class);

          for (String s : commands) {
            Grenadier.dispatch(c.getSource(), s);
          }

          return SINGLE_SUCCESS;
        })
    );
  }
}
