package net.arcadiusmc.items;

import net.arcadiusmc.ItemGraveService;
import net.arcadiusmc.items.listeners.ItemCallbackListeners;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    ItemTypes.registerAll();
    Events.register(new ItemCallbackListeners());

    ItemGraveService service = ItemGraveService.grave();
    service.addFilter("items_plugin", PluginGraveFilter.FILTER);

    reloadConfig();
  }

  @Override
  public void onDisable() {
    ItemGraveService service = ItemGraveService.grave();
    service.removeFilter("items_plugin");

    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
  }
}
