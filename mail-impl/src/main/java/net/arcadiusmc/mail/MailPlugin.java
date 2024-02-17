package net.arcadiusmc.mail;

import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.mail.command.MailCommands;
import net.arcadiusmc.mail.listeners.MailListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

public class MailPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Getter
  private ServiceImpl service;
  private PeriodicalSaver saver;

  @Getter
  private MailConfig mailConfig;

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    service = new ServiceImpl(this);
    BukkitServices.register(MailService.class, service);

    reloadConfig();

    MailCommands.createCommands(service);
    MailListeners.registerAll(service);

    service.load();

    saver = PeriodicalSaver.create(service::save, () -> Duration.ofMinutes(30));
    saver.start();

    var server = ArcadiusServer.server();
    MailPrefs.init(server.getGlobalSettingsBook());
  }

  @Override
  public void onDisable() {
    service.save();
    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    mailConfig = TomlConfigs.loadPluginConfig(this, MailConfig.class);
    MessageLoader.loadPluginMessages(this, messageList);
  }
}
