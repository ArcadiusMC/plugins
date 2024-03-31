package net.arcadiusmc.usables.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.usables.UPermissions;
import net.arcadiusmc.usables.UsablesPlugin;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;

public class CommandUsables extends BaseCommand {

  private final UsablesPlugin plugin;

  public CommandUsables(UsablesPlugin plugin) {
    super("usable");

    setAliases("interactable", "usables");
    setPermission(UPermissions.USABLES);

    this.plugin = plugin;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("reload")
        .addInfo("Reloads the usables plugin");

    factory.usage("save")
        .addInfo("Saves the usables plugin's data.");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(literal("reload")
        .executes(c -> {
          plugin.reload();
          c.getSource().sendSuccess(Component.text("Reloaded Usables plugin"));
          return 0;
        })
    );

    command.then(literal("save")
        .executes(c -> {
          plugin.save();
          c.getSource().sendSuccess(Component.text("Saved Usables plugin"));
          return 0;
        })
    );
  }
}
