package net.arcadiusmc.items;

import lombok.Getter;
import net.arcadiusmc.ItemGraveService;
import net.arcadiusmc.items.commands.ItemCommands;
import net.arcadiusmc.items.guns.GunTicker;
import net.arcadiusmc.items.guns.PlayerMoveSpeeds;
import net.arcadiusmc.items.listeners.ItemListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.io.ConfigCodec;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Getter
  private ItemsConfig itemsConfig;

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    GunTicker.TICKER.start();
    PlayerMoveSpeeds.SPEEDS.start();

    reloadConfig();

    ItemTypes.registerAll();
    ItemListeners.registerAll(this);
    ItemCommands.registerAll();

    ItemGraveService service = ItemGraveService.grave();
    service.addFilter("items_plugin", PluginGraveFilter.FILTER);
  }

  @Override
  public void onDisable() {
    ItemGraveService service = ItemGraveService.grave();
    service.removeFilter("items_plugin");

    Messages.MESSAGE_LIST.removeChild(getName());

    GunTicker.TICKER.stop();
    PlayerMoveSpeeds.SPEEDS.stop();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);

    this.itemsConfig = ConfigCodec.loadPluginConfig(this, ItemsConfig.CODEC)
        .orElse(ItemsConfig.DEFAULT);
  }
}
