package net.arcadiusmc.leaderboards;

import lombok.Getter;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.leaderboards.commands.LeaderboardCommands;
import net.arcadiusmc.leaderboards.listeners.PlayerListener;
import net.arcadiusmc.leaderboards.listeners.ServerListener;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LeaderboardPlugin extends JavaPlugin {

  private BoardsConfig boardsConfig;
  private ServiceImpl service;

  private PeriodicalSaver saver;

  static LeaderboardPlugin plugin() {
    return JavaPlugin.getPlugin(LeaderboardPlugin.class);
  }

  @Override
  public void onEnable() {
    service = new ServiceImpl(this);
    saver = PeriodicalSaver.create(service::save, () -> boardsConfig.autosaveInterval());

    BukkitServices.register(LeaderboardService.class, service);
    service.getTriggers().activate();
    service.createDefaultSources();

    Events.register(new ServerListener(this));
    Events.register(new PlayerListener(this));

    LeaderboardCommands.createCommands(this);
  }

  @Override
  public void onDisable() {
    service.getTriggers().close();
  }

  @Override
  public void reloadConfig() {
    this.boardsConfig = TomlConfigs.loadPluginConfig(this, BoardsConfig.class);
    saver.start();
  }

  public void reload() {
    reloadConfig();
    service.load();
  }
}
