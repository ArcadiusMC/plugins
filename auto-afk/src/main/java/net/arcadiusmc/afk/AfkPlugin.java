package net.arcadiusmc.afk;

import net.arcadiusmc.afk.commands.CommandAfk;
import net.arcadiusmc.afk.listeners.AfkListener;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import org.bukkit.plugin.java.JavaPlugin;

public class AfkPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
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
  }
}
