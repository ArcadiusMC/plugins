package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.CommandFile;

@CommandFile("commands/ftccore.gcn")
public class CommandFtcCore {

  void reload(CommandSource source) {
    CorePlugin plugin = CorePlugin.plugin();
    plugin.reload();

    source.sendSuccess(Messages.renderText("cmd.ftccore.reload.plugin", source));
  }

  void reloadConfig(CommandSource source) {
    CorePlugin plugin = CorePlugin.plugin();
    plugin.reloadConfig();

    source.sendSuccess(Messages.renderText("cmd.ftccore.reload.config", source));
  }

  void save(CommandSource source) {
    CorePlugin plugin = CorePlugin.plugin();
    plugin.save();

    source.sendSuccess(Messages.renderText("cmd.ftccore.saved.plugin", source));
  }
}
