package net.arcadiusmc.economy;

import com.sk89q.worldguard.WorldGuard;
import lombok.Getter;
import net.arcadiusmc.economy.market.MarketManager;
import net.arcadiusmc.economy.market.commands.MarketCommands;
import net.arcadiusmc.economy.market.listeners.MarketListener;
import net.arcadiusmc.economy.signshops.ShopManager;
import net.arcadiusmc.economy.signshops.SignShopFlags;
import net.arcadiusmc.economy.signshops.commands.SignShopCommands;
import net.arcadiusmc.economy.signshops.listeners.ShopListeners;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.utils.TomlConfigs;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ShopsPlugin extends JavaPlugin {

  private ShopConfig shopConfig;

  private MarketManager markets;
  private ShopManager shops;

  public static ShopsPlugin getPlugin() {
    return JavaPlugin.getPlugin(ShopsPlugin.class);
  }

  @Override
  public void onEnable() {
    markets = new MarketManager(this);
    shops = new ShopManager(this);
    reload();

    SignShopCommands.createCommands(shops);
    ShopListeners.registerAll(shops);

    MarketCommands.createCommands(markets);
    Events.register(new MarketListener(markets));
  }

  @Override
  public void onLoad() {
    SignShopFlags.register(WorldGuard.getInstance().getFlagRegistry());
  }

  @Override
  public void onDisable() {
    markets.save();
    shops.save();
  }

  public void reload() {
    reloadConfig();

    markets.load();
    shops.reload();
  }

  @Override
  public void reloadConfig() {
    shopConfig = TomlConfigs.loadPluginConfig(this, ShopConfig.class);
  }
}
