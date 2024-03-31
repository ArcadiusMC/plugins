package net.arcadiusmc.holograms;

import lombok.Getter;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.holograms.commands.HologramCommands;
import net.arcadiusmc.holograms.listeners.PlayerListener;
import net.arcadiusmc.holograms.listeners.ServerListener;
import net.arcadiusmc.holograms.placeholders.HologramPlaceholders;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class HologramPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private BoardsConfig boardsConfig;
  private ServiceImpl service;

  private PeriodicalSaver saver;

  static HologramPlugin plugin() {
    return JavaPlugin.getPlugin(HologramPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    service = new ServiceImpl(this);
    saver = PeriodicalSaver.create(service::save, () -> boardsConfig.autosaveInterval());

    BukkitServices.register(HologramService.class, service);
    service.getTriggers().activate();
    service.createDefaultSources();

    Events.register(new ServerListener(this));
    Events.register(new PlayerListener(this));

    HologramCommands.createCommands(this);
    HologramPlaceholders.registerAll();
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild(getName());
    service.getTriggers().close();
    HologramPlaceholders.unregister();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);

    this.boardsConfig = TomlConfigs.loadPluginConfig(this, BoardsConfig.class);
    saver.start();
  }

  public void reload() {
    reloadConfig();
    service.load();
  }
}
