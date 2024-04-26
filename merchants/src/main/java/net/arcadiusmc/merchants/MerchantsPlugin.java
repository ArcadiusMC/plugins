package net.arcadiusmc.merchants;

import lombok.Getter;
import net.arcadiusmc.merchants.commands.MerchantCommands;
import net.arcadiusmc.merchants.listeners.MerchantListeners;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class MerchantsPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  @Getter
  private final Registry<Merchant> merchants = Registries.newRegistry();

  @Override
  public void onEnable() {
    Messages.MESSAGE_LIST.addChild(getName(), messageList);

    ParrotMerchant parrotMerchant = new ParrotMerchant(this);
    EnchantsMerchant enchantsMerchant = new EnchantsMerchant(this);

    merchants.register("parrots", parrotMerchant);
    merchants.register("enchants", enchantsMerchant);

    MerchantCommands.createCommands(this);
    MerchantListeners.registerAll(this);

    reloadConfig();
    load();

    for (Merchant merchant : merchants) {
      merchant.onEnable();
    }
  }

  @Override
  public void onDisable() {
    Messages.MESSAGE_LIST.removeChild(getName());
    save();
  }

  public void load() {
    for (Merchant merchant : merchants) {
      merchant.load();
    }
  }

  public void save() {
    for (Merchant merchant : merchants) {
      merchant.save();
    }
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this, messageList);

    for (Merchant merchant : merchants) {
      merchant.reloadConfig();
    }
  }
}
