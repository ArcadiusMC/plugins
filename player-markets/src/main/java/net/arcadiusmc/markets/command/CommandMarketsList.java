package net.arcadiusmc.markets.command;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.markets.gui.ShopListMenu;
import net.arcadiusmc.markets.gui.ShopLists;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMarketsList extends BaseCommand {

  private final ShopLists lists;

  public CommandMarketsList(ShopLists lists) {
    super("open-market-list");
    this.lists = lists;
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    var argument = new RegistryArguments<>(lists.getRegistry(), "Market-List");

    command
        .then(argument("menu", argument)
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> {
                  User player = Arguments.getUser(c, "player");
                  Holder<ShopListMenu> holder = c.getArgument("menu", Holder.class);
                  holder.getValue().getMenu().open(player);
                  return 0;
                })
            )
        );
  }
}
