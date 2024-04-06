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

  @Getter
  private Afk afk;

  private final MessageList messageList = MessageList.create();

  public static AfkPlugin plugin() {
    return getPlugin(AfkPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    reloadConfig();

    afk = new Afk(this);

    new CommandAfk(this);
    Events.register(new AfkListener(afk));

    UserNameFactory factory = Users.getService().getNameFactory();
    factory.addSuffix("afk.suffix", 1, new AfkNameElement(afk));
    factory.addProfileField("afk.reason", 31, new AfkProfileField(afk));

    afk.startTicking();
  }

  @Override
  public void onDisable() {
    afk.stopTicking();

    UserNameFactory factory = Users.getService().getNameFactory();
    factory.removeSuffix("afk.suffix");
    factory.removeField("afk.reason");

    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    afkConfig = TomlConfigs.loadPluginConfig(this, AfkConfig.class);
  }
}
