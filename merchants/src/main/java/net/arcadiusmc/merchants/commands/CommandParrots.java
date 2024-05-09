package net.arcadiusmc.merchants.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.merchants.ParrotMerchant;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandParrots extends BaseCommand {

  private final ParrotMerchant merchant;

  public CommandParrots(ParrotMerchant merchant) {
    super("parrots");

    this.merchant = merchant;

    setDescription("Opens the Parrots menu");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          Context context = ParrotMerchant.SET.createContext()
              .set(ParrotMerchant.PURCHASE_ALLOWED, false);

          merchant.getMenu().open(user, context);
          return 0;
        });
  }
}
