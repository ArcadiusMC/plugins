package net.arcadiusmc.mail;

import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.FtcServer;
import net.arcadiusmc.mail.command.MailCommands;
import net.arcadiusmc.mail.listeners.MailListeners;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

public class MailPlugin extends JavaPlugin {

  @Getter
  private ServiceImpl service;
  private PeriodicalSaver saver;

  @Getter
  private MailConfig mailConfig;

  @Override
  public void onEnable() {
    service = new ServiceImpl(this);
    BukkitServices.register(MailService.class, service);

    reloadConfig();

    MailCommands.createCommands(service);
    MailListeners.registerAll(service);

    service.load();

    saver = PeriodicalSaver.create(service::save, () -> Duration.ofMinutes(30));
    saver.start();

    var server = FtcServer.server();
    MailPrefs.init(server.getGlobalSettingsBook());
  }

  @Override
  public void onDisable() {
    service.save();
  }

  @Override
  public void reloadConfig() {
    mailConfig = TomlConfigs.loadPluginConfig(this, MailConfig.class);
  }
}
