package net.arcadiusmc.marriages;

import net.arcadiusmc.FtcServer;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.marriages.commands.CommandDivorce;
import net.arcadiusmc.marriages.commands.CommandMarriageAccept;
import net.arcadiusmc.marriages.commands.CommandMarriageChat;
import net.arcadiusmc.marriages.commands.CommandMarriageDeny;
import net.arcadiusmc.marriages.commands.CommandMarry;
import net.arcadiusmc.marriages.listeners.ChatListener;
import net.arcadiusmc.marriages.listeners.MarriageListener;
import net.arcadiusmc.user.Users;
import org.bukkit.plugin.java.JavaPlugin;

public class MarriagePlugin extends JavaPlugin {

  @Override
  public void onEnable() {

    new CommandDivorce();
    new CommandMarriageAccept();
    new CommandMarriageChat();
    new CommandMarriageDeny();
    new CommandMarry();

    Events.register(new MarriageListener());
    Events.register(new ChatListener());

    var ftcServer = FtcServer.server();
    Marriages.defineSettings(ftcServer.getGlobalSettingsBook());

    var nameFactory = Users.getService().getNameFactory();
    nameFactory.addProfileField("spouse", 35, new SpouseProfileElement());
  }

  @Override
  public void onDisable() {
    var nameFactory = Users.getService().getNameFactory();
    nameFactory.removeField("spouse");
  }
}
