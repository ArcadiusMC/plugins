package net.arcadiusmc.vault;

import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.utils.PluginUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    if (!PluginUtil.isEnabled("Vault")) {
      getSLF4JLogger().error("Vault plugin not found, disabling");
      getServer().getPluginManager().disablePlugin(this);

      return;
    }

    UserService service = Users.getService();
    for (Holder<Currency> entry : service.getCurrencies().entries()) {
      EconomyInterface econ = new EconomyInterface(entry.getKey(), entry.getValue());
      econ.registerService(this);
    }

    getSLF4JLogger().debug("Registered economy service");
  }

  @Override
  public void onDisable() {

  }
}
