package net.arcadiusmc.merchants.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.merchants.Merchant;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMerchantMenu extends BaseCommand {

  public CommandMerchantMenu() {
    super("open-merchant-menu");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("menu", MerchantCommands.argument)
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> {
                  User user = Arguments.getUser(c, "player");

                  Holder<Merchant> holder = c.getArgument("menu", Holder.class);
                  Merchant merchant = holder.getValue();

                  if (merchant.getMenu() == null) {
                    throw Exceptions.create("Internal Error: Merchant has no menu");
                  }

                  merchant.getMenu().open(user);
                  return 0;
                })
            )
        );
  }
}
