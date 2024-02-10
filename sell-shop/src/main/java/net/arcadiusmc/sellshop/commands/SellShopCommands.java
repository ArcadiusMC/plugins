package net.arcadiusmc.sellshop.commands;

import net.arcadiusmc.command.Commands;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.arcadiusmc.sellshop.SellShop;

public final class SellShopCommands {
  private SellShopCommands() {}

  public static void createCommands(SellShop shop) {
    SellMaterialArgument arg = new SellMaterialArgument(shop.getPriceMap());

    new CommandsShop(shop);
    new CommandSell(shop, arg);

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.getVariables().put("material", arg);

    ctx.registerCommand(new CommandAutoSell());
    ctx.registerCommand(new CommandEarnings());
  }
}
