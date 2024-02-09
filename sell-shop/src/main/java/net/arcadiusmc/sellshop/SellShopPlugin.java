package net.arcadiusmc.sellshop;

import lombok.Getter;
import net.arcadiusmc.sellshop.commands.SellShopCommands;
import net.arcadiusmc.sellshop.listeners.SellShopListeners;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.utils.io.PathUtil;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class SellShopPlugin extends JavaPlugin {

  private SellShopConfig shopConfig;

  private SellShop sellShop;

  public static SellShopPlugin getPlugin() {
    return getPlugin(SellShopPlugin.class);
  }

  @Override
  public void onEnable() {
    SellProperties.registerAll();

    sellShop = new SellShop(this, PathUtil.pluginPath());
    reloadConfig();

    SellShopCommands.createCommands(sellShop);
    SellShopListeners.registerAll(this);

    Users.getService().registerComponent(UserShopData.class);
  }

  @Override
  public void onDisable() {

  }

  @Override
  public void reloadConfig() {
    shopConfig = TomlConfigs.loadPluginConfig(this, SellShopConfig.class);
    sellShop.load();
  }
}
