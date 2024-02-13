package net.arcadiusmc.sellshop.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.sellshop.SellShop;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.arcadiusmc.sellshop.loader.SellShopPage;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandOpenPage extends BaseCommand {

  private final SellShopPlugin plugin;

  public CommandOpenPage(SellShopPlugin plugin) {
    super("open-sellshop-menu");

    this.plugin = plugin;

    setDescription("Opens a specific sellshop page for a player");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    RegistryArguments<SellShopPage> argumentType
        = new RegistryArguments<>(plugin.getSellShop().getPages(), "Sell Shop Page");

    command
        .then(argument("page", argumentType)
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> {
                  User user = Arguments.getUser(c, "player");
                  Holder<SellShopPage> holder = c.getArgument("page", Holder.class);
                  SellShopPage page = holder.getValue();
                  page.getMenu().open(user, SellShop.SET.createContext());
                  return 0;
                })
            )
        );
  }
}
