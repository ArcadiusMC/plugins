package net.arcadiusmc.merchants;

import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import org.bukkit.plugin.java.JavaPlugin;

public class MerchantsPlugin extends JavaPlugin {

  private final Registry<Merchant> merchants = Registries.newRegistry();

  @Override
  public void onEnable() {
    ParrotMerchant parrotMerchant = new ParrotMerchant(this);

    merchants.register("parrots", parrotMerchant);

    load();
  }

  @Override
  public void onDisable() {
    save();
  }

  public void load() {
    for (Merchant merchant : merchants) {
      merchant.load();
    }
  }

  public void save() {
    for (Merchant merchant : merchants) {
      merchant.saveData();
    }
  }

  @Override
  public void reloadConfig() {
    for (Merchant merchant : merchants) {
      merchant.reloadConfig();
    }
  }
}
