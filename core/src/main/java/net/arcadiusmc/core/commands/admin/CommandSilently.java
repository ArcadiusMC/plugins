package net.arcadiusmc.core.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandSilently extends BaseCommand {

  public CommandSilently() {
    super("silently");

    setDescription("Executes another command silently, with no output");
    setAliases("with-no-output", "run-silent", "quietly", "run-quietly");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<command>")
        .addInfo("Executes a command and silences all output.")
        .addInfo("Useful for when commands are meant to be executed")
        .addInfo("automatically with no output.");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("cmd", StringArgumentType.greedyString())
        .suggests(Grenadier.suggestAllCommands())

        .executes(c -> {
          String cmd = StringArgumentType.getString(c, "cmd");
          CommandSource source = c.getSource().silent();
          return Grenadier.dispatch(source, cmd);
        })
    );
  }
}
