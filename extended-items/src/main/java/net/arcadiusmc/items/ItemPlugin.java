package net.arcadiusmc.items;

import lombok.Getter;
import net.arcadiusmc.ItemGraveService;
import net.arcadiusmc.items.commands.ItemCommands;
import net.arcadiusmc.items.guns.GunTicker;
import net.arcadiusmc.items.guns.PlayerMoveSpeeds;
import net.arcadiusmc.items.listeners.ItemListeners;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.io.ConfigCodec;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemPlugin extends JavaPlugin {

  @Getter
  private ItemsConfig itemsConfig;

  @Getter
  private NonNatural nonNatural;

  @Override
  public void onEnable() {
    GunTicker.TICKER.start();
    PlayerMoveSpeeds.SPEEDS.start();

    reloadConfig();
    nonNatural = new NonNatural(getDataPath().resolve("natural-tracker"));

    ArcadiusEnchantments.findEnchantments();
    ItemTypes.registerAll();
    ItemListeners.registerAll(this);
    ItemCommands.registerAll();

    ItemGraveService service = ItemGraveService.grave();
    service.addFilter("items_plugin", PluginGraveFilter.FILTER);
    service.addFilter("soulbound", SoulboundGraveFilter.FILTER);
  }

  @Override
  public void onDisable() {
    ItemGraveService service = ItemGraveService.grave();
    service.removeFilter("items_plugin");
    service.removeFilter("soulbound");

    GunTicker.TICKER.stop();
    PlayerMoveSpeeds.SPEEDS.stop();

    if (nonNatural != null) {
      nonNatural.save();
    }
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);

    this.itemsConfig = ConfigCodec.loadPluginConfig(this, ItemsConfig.CODEC)
        .orElse(ItemsConfig.DEFAULT);
  }
}
