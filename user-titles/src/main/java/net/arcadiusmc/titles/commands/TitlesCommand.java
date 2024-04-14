package net.arcadiusmc.titles.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.titles.RankMenu;
import net.arcadiusmc.titles.TitlesPlugin;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.CommandFile;
import org.bukkit.plugin.java.JavaPlugin;

@CommandFile("titles_command.gcn")
public class TitlesCommand {

  static final String ADMIN_PERMISSION = "arcadius.commands.ranks.admin";

  void openMenu(CommandSource source) throws CommandSyntaxException {
    User user = Commands.getUserSender(source);
    RankMenu.getInstance().open(user);
  }

  void reloadPlugin(CommandSource source) {
    TitlesPlugin plugin = JavaPlugin.getPlugin(TitlesPlugin.class);

    plugin.load();

    source.sendSuccess(Messages.renderText("cmd.ranks.reloaded.plugin", source));
  }

  void reloadConfig(CommandSource source) {
    TitlesPlugin plugin = JavaPlugin.getPlugin(TitlesPlugin.class);

    plugin.reloadConfig();

    source.sendSuccess(Messages.renderText("cmd.ranks.reloaded.config", source));
  }
}