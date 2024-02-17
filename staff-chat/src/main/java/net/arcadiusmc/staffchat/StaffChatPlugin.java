package net.arcadiusmc.staffchat;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

public class StaffChatPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Getter
  private StaffChatConfig scConfig = new StaffChatConfig();

  public static StaffChatPlugin plugin() {
    return getPlugin(StaffChatPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    new CommandStaffChat();

    if (PluginUtil.isEnabled("Arcadius-DiscordHook")) {
      DiscordSRV.api.subscribe(new StaffChatDiscordListener(this));
    }

    SettingsBook<User> settingsBook = ArcadiusServer.server().getGlobalSettingsBook();
    StaffChat.createSettings(settingsBook);

    reloadConfig();
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    scConfig = TomlConfigs.loadPluginConfig(this, StaffChatConfig.class);
  }
}
