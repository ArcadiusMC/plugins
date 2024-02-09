package net.arcadiusmc.usables.commands;

import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.UsablesPlugin;

public class UsablesCommands {

  static UsableCommand[] usableCommands;

  static ListCommands<Action> actions;
  static ListCommands<Condition> conditions;

  public static void createCommands(UsablesPlugin plugin) {
    actions = new ListCommands<>("actions", "Action", plugin.getActions());
    conditions = new ListCommands<>("tests", "Condition", plugin.getConditions());

    usableCommands = new UsableCommand[]{
        new UsableBlockCommand(),
        new UsableEntityCommand(),
        new UsableItemCommand(),
        new UsableTriggerCommand(plugin.getTriggers()),
        new KitCommand(plugin.getKits()),
        new WarpCommand(plugin.getWarps())
    };

    for (UsableCommand usableCommand : usableCommands) {
      usableCommand.register();
    }

    new CommandUsables(plugin).register();
  }
}
