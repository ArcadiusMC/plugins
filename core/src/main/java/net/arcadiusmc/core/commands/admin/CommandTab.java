package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;
import net.kyori.adventure.text.Component;

@CommandFile("commands/tab.gcn")
public class CommandTab {

  private final CorePlugin plugin;

  public CommandTab(CorePlugin plugin) {
    this.plugin = plugin;
  }

  void update(CommandSource source) {
    plugin.getTabMenu().update();
    source.sendSuccess(Messages.renderText("cmd.tab.updated", source));
  }

  void reload(CommandSource source) {
    plugin.getTabMenu().load();
    source.sendSuccess(Messages.renderText("cmd.tab.reloaded", source));
  }

  void setSuffix(CommandSource source, @Argument("user") User user, @Argument("text") Component text) {
    set(source, user, "suffix", Properties.SUFFIX, text);
  }

  void unsetSuffix(CommandSource source, @Argument("user") User user) {
    unset(source, user, "suffix", Properties.SUFFIX);
  }

  void setPrefix(CommandSource source, @Argument("user") User user, @Argument("text") Component text) {
    set(source, user, "prefix", Properties.PREFIX, text);
  }

  void unsetPrefix(CommandSource source, @Argument("user") User user) {
    unset(source, user, "prefix", Properties.PREFIX);
  }

  void setName(CommandSource source, @Argument("user") User user, @Argument("text") Component text) {
    set(source, user, "displayName", Properties.TAB_NAME, text);
  }

  void unsetName(CommandSource source, @Argument("user") User user) {
    unset(source, user, "displayName", Properties.TAB_NAME);
  }

  void unset(
      CommandSource source,
      User user,
      String messageKey,
      UserProperty<Component> property
  ) {
    user.set(property, null);

    source.sendMessage(
        Messages.render("cmd.tab.unset." + messageKey)
            .addValue("player", user)
            .create(source)
    );
  }

  void set(
      CommandSource source,
      User user,
      String messageKey,
      UserProperty<Component> property,
      Component text
  ) {
    if (Text.isDashClear(text)) {
      unset(source, user, messageKey, property);
      return;
    }

    user.set(property, text);

    source.sendMessage(
        Messages.render("cmd.tab.set", messageKey)
            .addValue("player", user)
            .addValue("value", text)
            .create(source)
    );
  }
}
