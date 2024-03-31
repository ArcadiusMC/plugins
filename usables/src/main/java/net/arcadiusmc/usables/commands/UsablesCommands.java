package net.arcadiusmc.usables.commands;

import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.UsablesPlugin;

public class UsablesCommands {

  static ListCommands<Action> actions;
  static ListCommands<Condition> conditions;

  public static void createCommands(UsablesPlugin plugin) {
    actions = new ListCommands<>("actions", "Action", plugin.getActions());
    conditions = new ListCommands<>("tests", "Condition", plugin.getConditions());

    new UsableBlockCommand().register();
    new UsableEntityCommand().register();
    new UsableItemCommand().register();
    new UsableTriggerCommand(plugin.getTriggers()).register();
    new UsableVirtualCommand().register();
    new KitCommand(plugin.getKits()).register();
    new WarpCommand(plugin.getWarps()).register();

    new CommandUsables(plugin).register();
  }
}
