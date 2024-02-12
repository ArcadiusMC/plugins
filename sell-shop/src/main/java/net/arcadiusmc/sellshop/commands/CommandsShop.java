package net.arcadiusmc.sellshop.commands;

import java.util.Optional;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.sellshop.SellPermissions;
import net.arcadiusmc.sellshop.SellShop;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.arcadiusmc.sellshop.loader.SellShopPage;
import net.arcadiusmc.text.Messages;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandsShop extends BaseCommand {

  private final SellShopPlugin plugin;

  public CommandsShop(SellShopPlugin plugin) {
    super("shop");

    this.plugin = plugin;

    setAliases("sellshop");
    setDescription("Opens the server's sell shop");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          CommandSource source = c.getSource();

          String mainPageName = this.plugin.getShopConfig().mainPageName();
          SellShop shop = this.plugin.getSellShop();

          Optional<SellShopPage> pageOpt = shop.getPages().get(mainPageName);

          if (pageOpt.isEmpty()) {
            throw Messages.render("sellshop.mainPageMissing").exception(source);
          }

          SellShopPage page = pageOpt.get();
          page.getMenu().open(getUserSender(c), SellShop.SET.createContext());

          return 0;
        })

        .then(literal("reload")
            .requires(source -> source.hasPermission(SellPermissions.SHOP_ADMIN))

            .executes(c -> {
              SellShopPlugin.getPlugin().reloadConfig();
              c.getSource().sendSuccess(Messages.renderText("sellshop.reloaded", c.getSource()));
              return 0;
            })
        );
  }
}
