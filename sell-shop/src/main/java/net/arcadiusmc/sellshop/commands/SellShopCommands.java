package net.arcadiusmc.sellshop.commands;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;

public final class SellShopCommands {
  private SellShopCommands() {}

  public static void createCommands(SellShopPlugin plugin) {
    SellMaterialArgument arg = new SellMaterialArgument(plugin.getDataSource().getGlobalPrices());

    new CommandsShop(plugin);

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.getVariables().put("material", arg);

    ctx.registerCommand(new CommandAutoSell());
    ctx.registerCommand(new CommandEarnings());
  }
}
