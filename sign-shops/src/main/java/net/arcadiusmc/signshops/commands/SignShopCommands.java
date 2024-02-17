package net.arcadiusmc.signshops.commands;

import net.arcadiusmc.signshops.ShopManager;

public class SignShopCommands {

  public static void createCommands(ShopManager manager) {
    new CommandEditShop(manager);
    new CommandShopReselling(manager);
    new CommandShopHistory(manager);
  }
}
