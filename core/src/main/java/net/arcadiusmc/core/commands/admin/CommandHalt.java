package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandHalt extends BaseCommand {

  public CommandHalt() {
    super("syshalt");

    setAliases("haltsys", "halt");
    setDescription("Initiates an instant system shutdown. This should only be available on test servers");

    if (Loggers.getLogger().isDebugEnabled()) {
      register();
    }
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      if (!Loggers.getLogger().isDebugEnabled())   {
        throw Exceptions.create("Not available in non-debug mode");
      }

      PrintStream ps = new PrintStream(new FileOutputStream(FileDescriptor.out));
      ps.println(">>");
      ps.println(">> System halt command called by: " + c.getSource().textName());
      ps.println(">>");

      Runtime.getRuntime().halt(1);
      return SINGLE_SUCCESS;
    });
  }
}
