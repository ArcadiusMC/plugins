package net.arcadiusmc.afk;

import lombok.Getter;
import net.arcadiusmc.afk.commands.CommandAfk;
import net.arcadiusmc.afk.listeners.AfkListener;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

public class AfkPlugin extends JavaPlugin {

  @Getter
  private AfkConfig afkConfig;

  private final MessageList messageList = MessageList.create();

  static AfkPlugin plugin() {
    return getPlugin(AfkPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild("afk", messageList);

    reloadConfig();

    new CommandAfk();
    Events.register(new AfkListener());

    UserNameFactory factory = Users.getService().getNameFactory();
    factory.addSuffix("afk.suffix", 1, new AfkNameElement());
    factory.addProfileField("afk.reason", 31, new AfkProfileField());
  }

  @Override
  public void onDisable() {
    UserNameFactory factory = Users.getService().getNameFactory();
    factory.removeSuffix("afk.suffix");
    factory.removeField("afk.reason");

    Messages.MESSAGE_LIST.removeChild("afk");
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    afkConfig = TomlConfigs.loadPluginConfig(this, AfkConfig.class);
  }
}
