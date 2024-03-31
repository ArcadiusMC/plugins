package net.arcadiusmc.structure.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.structure.FunctionInfo;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandStructFunction extends BaseCommand {

  public static final String COMMAND_NAME = "StructFunction";

  public CommandStructFunction() {
    super(COMMAND_NAME);
    setDescription("Ignore this command");
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /StructFunction
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("args", FunctionInfo.PARSER));
  }
}