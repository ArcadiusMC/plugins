package net.arcadiusmc.economy.signshops.commands;

import net.arcadiusmc.economy.signshops.ShopManager;

public class SignShopCommands {

  public static void createCommands(ShopManager manager) {
    new CommandEditShop(manager);
    new CommandShopReselling(manager);
    new CommandShopHistory(manager);
  }
}
