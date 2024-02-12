package net.arcadiusmc.sellshop;

import lombok.Getter;
import net.arcadiusmc.sellshop.commands.SellShopCommands;
import net.arcadiusmc.sellshop.data.ItemDataSource;
import net.arcadiusmc.sellshop.listeners.SellShopListeners;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class SellShopPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();
  private SellShopConfig shopConfig;
  private SellShop sellShop;
  private ItemDataSource dataSource;

  public static SellShopPlugin getPlugin() {
    return getPlugin(SellShopPlugin.class);
  }

  @Override
  public void onEnable() {

    Messages.MESSAGE_LIST.addChild("sellshop", this.messageList);
    SellProperties.registerAll();

    this.sellShop = new SellShop(this);
    this.dataSource = new ItemDataSource(this);

    this.reloadConfig();

    SellShopCommands.createCommands(this);
    SellShopListeners.registerAll(this);

    Users.getService().registerComponent(UserShopData.class);
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild("sellshop");
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);
    shopConfig = TomlConfigs.loadPluginConfig(this, SellShopConfig.class);

    dataSource.load();
    sellShop.load();
  }
}
