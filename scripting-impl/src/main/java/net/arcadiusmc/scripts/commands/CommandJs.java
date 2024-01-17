package net.arcadiusmc.scripts.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.utils.io.source.Source;
import net.arcadiusmc.utils.io.source.Sources;

public class CommandJs extends BaseCommand {

  public CommandJs() {
    super("js");
    setDescription("Runs JavaScript code");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<code>", "Runs JavaScript code");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("code", StringArgumentType.greedyString())
        .executes(c -> {
          var code = c.getArgument("code", String.class);
          Source scriptSource = Sources.direct(code, "<command script>");
          ScriptingCommand.executeScript(c.getSource(), scriptSource, false, null);

          return 0;
        })
    );
  }
}
