package net.arcadiusmc.voicechat;

import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HookVcCommand extends BaseCommand {

  private final HookPlugin plugin;

  public HookVcCommand(HookPlugin plugin) {
    super("vc-hook-reload");

    this.plugin = plugin;

    setDescription("Reloads the Arcadius Simple Voice Chat Hook config");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      plugin.reloadConfig();
      c.getSource().sendSuccess(Component.text("Reloaded!", NamedTextColor.GRAY));
      return 0;
    });
  }
}
