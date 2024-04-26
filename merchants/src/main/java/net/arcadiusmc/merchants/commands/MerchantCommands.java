package net.arcadiusmc.merchants.commands;

import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.merchants.Merchant;
import net.arcadiusmc.merchants.MerchantsPlugin;

public class MerchantCommands {

  static RegistryArguments<Merchant> argument;

  public static void createCommands(MerchantsPlugin plugin) {
    argument = new RegistryArguments<>(plugin.getMerchants(), "Merchant");

    new CommandMerchants(plugin);
    new CommandMerchantMenu();
  }
}
