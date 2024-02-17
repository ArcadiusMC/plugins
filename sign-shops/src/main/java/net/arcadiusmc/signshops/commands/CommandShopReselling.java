package net.arcadiusmc.signshops.commands;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.signshops.SMessages;
import net.arcadiusmc.signshops.ShopManager;
import net.arcadiusmc.signshops.SignShop;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandShopReselling extends BaseCommand {

  private final ShopManager manager;

  public CommandShopReselling(ShopManager manager) {
    super("shopreselling");

    this.manager = manager;

    setAliases("shopresell");
    setDescription("Shop reselling");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      CommandSource source = c.getSource();
      SignShop shop = CommandEditShop.getShop(manager, source.asPlayer());
      boolean state = !shop.isResellDisabled();

      shop.setResellDisabled(state);
      shop.update();

      source.sendMessage(SMessages.resellToggle(source, state));
      return 0;
    });
  }
}
