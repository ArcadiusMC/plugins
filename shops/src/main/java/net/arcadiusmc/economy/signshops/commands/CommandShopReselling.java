package net.arcadiusmc.economy.signshops.commands;

import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.economy.signshops.ShopManager;
import net.arcadiusmc.economy.signshops.SignShop;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.text.Messages;

public class CommandShopReselling extends FtcCommand {

  private final ShopManager manager;

  public CommandShopReselling(ShopManager manager) {
    super("shopreselling");

    this.manager = manager;

    setAliases("shopresell");
    setPermission(Permissions.DEFAULT);
    setDescription("Shop reselling");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      SignShop shop = CommandEditShop.getShop(manager, c.getSource().asPlayer());
      boolean state = !shop.isResellDisabled();

      shop.setResellDisabled(state);
      shop.update();

      c.getSource().sendMessage(Messages.toggleMessage("Shop reselling {3}", state));
      return 0;
    });
  }
}
