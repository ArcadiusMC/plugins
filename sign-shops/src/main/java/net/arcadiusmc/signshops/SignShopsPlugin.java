package net.arcadiusmc.signshops;

import com.sk89q.worldguard.WorldGuard;
import lombok.Getter;
import net.arcadiusmc.signshops.commands.SignShopCommands;
import net.arcadiusmc.signshops.listeners.ShopListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class SignShopsPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private ShopManager manager;
  private ShopConfig shopConfig = new ShopConfig();

  public static SignShopsPlugin plugin() {
    return getPlugin(SignShopsPlugin.class);
  }

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    manager = new ShopManager(this);

    ShopListeners.registerAll(manager);
    SignShopCommands.createCommands(manager);

    reloadConfig();
  }

  @Override
  public void onLoad() {
    SignShopFlags.register(WorldGuard.getInstance().getFlagRegistry());
  }

  @Override
  public void onDisable() {
    if (manager != null) {
      manager.save();
    }

    Messages.MESSAGE_LIST.removeChild(getName());
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    shopConfig = TomlConfigs.loadPluginConfig(this, ShopConfig.class);
  }
}
