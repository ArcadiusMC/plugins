package net.arcadiusmc.signshops.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.signshops.SExceptions;
import net.arcadiusmc.signshops.SMessages;
import net.arcadiusmc.signshops.SPermissions;
import net.arcadiusmc.signshops.ShopManager;
import net.arcadiusmc.signshops.ShopType;
import net.arcadiusmc.signshops.SignShop;
import net.arcadiusmc.signshops.SignShops;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandEditShop extends BaseCommand {

  private final ShopManager manager;

  public CommandEditShop(ShopManager manager) {
    super("editshop");

    this.manager = manager;

    setDescription("Allows you to edit a shop");
    setAliases("shopedit", "signshop");
    setPermission(SPermissions.EDIT);

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Displays help information");

    factory.usage("buy", "Makes the shop you're looking at a buy shop");
    factory.usage("sell", "Makes the shop you're looking at a sell shop");

    factory.usage("line <2 | 3> <text>")
        .addInfo("Changes either the 2nd or 3rd line of the")
        .addInfo("sign shop you're looking at");

    factory.usage("amount <amount: 1..64>")
        .addInfo("Changes the amount of items the shop sells/buys");

    factory.usage("price <value: 1..>")
        .addInfo("Changes the price of the shop you're looking at");

    factory.usage("info")
        .addInfo("Displays info about the shop you're looking at")
        .setPermission(SPermissions.ADMIN);
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("info")
            .requires(source -> source.hasPermission(SPermissions.ADMIN))

            .executes(c -> {
              Player player = c.getSource().asPlayer();
              var shop = getShop(player);

              var writer = TextWriters.newWriter();
              writer.setFieldStyle(Style.style(NamedTextColor.GRAY));

              writer.field("Type", shop.getType().name().toLowerCase());

              writer.field(
                  "Price",
                  Text.format("{0, rhines}", shop.getPrice())
              );

              if (shop.getOwner() != null) {
                writer.field(
                    "Owner",
                    Text.format("{0, user}", shop.getOwner())
                );
              }

              writer.field("Item", Text.itemAndAmount(shop.getExampleItem()));

              if (!shop.getType().isAdmin()) {
                writer.field("Left in stock", Text.formatNumber(shop.itemCount()));
              }

              player.sendMessage(writer.asComponent());
              return 0;
            })
        )

        .then(literal("buy").executes(c -> setType(c, false)))
        .then(literal("sell").executes(c -> setType(c, true)))

        .then(literal("price")
            .then(argument("price_actual", IntegerArgumentType.integer(0))
                .executes(c -> {
                  Player player = c.getSource().asPlayer();
                  SignShop shop = getShop(player);

                  int price = c.getArgument("price_actual", Integer.class);
                  shop.setPrice(price);

                  player.sendMessage(SMessages.shopEditPrice(player, price));

                  updateShop(shop);
                  return 0;
                })
            )
        )
        .then(literal("amount")
            .then(argument("amount_actual", IntegerArgumentType.integer(1, 64))
                .executes(c -> {
                  Player player = c.getSource().asPlayer();
                  SignShop shop = getShop(player);

                  int amount = c.getArgument("amount_actual", Integer.class);
                  ItemStack exampleItem = shop.getExampleItem();

                  if (amount > exampleItem.getMaxStackSize()) {
                    StringReader reader = new StringReader(c.getInput());
                    reader.setCursor(c.getInput().indexOf(amount + ""));

                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                        .integerTooHigh()
                        .createWithContext(
                            reader, amount,
                            exampleItem.getMaxStackSize()
                        );
                  }

                  exampleItem.setAmount(amount);
                  shop.setExampleItem(exampleItem);

                  player.sendMessage(SMessages.shopEditAmount(player, exampleItem));

                  updateShop(shop);
                  return 0;
                })
            )
        )
        /*.then(literal("transfer")
            .then(argument("player_transfer", Arguments.USER)

                .executes(c -> {
                  User user = getUserSender(c);
                  SignShop shop = getShop(user.getPlayer());
                  User target = Arguments.getUser(c, "player_transfer");

                  if (user.equals(target) && !user.hasPermission(Permissions.SHOP_ADMIN)) {
                    throw Exceptions.TRANSFER_SELF;
                  }

                  shop.setOwner(target.getUniqueId());

                  user.sendMessage(Messages.shopTransferSender(target));
                  target.sendMessage(Messages.shopTransferTarget(user, shop));

                  updateShop(shop);
                  return 0;
                })
            )
        )*/
        .then(literal("line")
            .then(argument("line_actual", IntegerArgumentType.integer(2, 3))
                .suggests((context, builder) -> {
                  return Completions.suggest(builder, "2", "3");
                })

                .then(argument("line_text", Arguments.MESSAGE)
                    .suggests((context, builder) -> {
                      if (builder.getRemainingLowerCase().startsWith("-c")) {
                        builder.suggest("-clear");
                        return builder.buildFuture();
                      }

                      return Arguments.MESSAGE.listSuggestions(context, builder);
                    })

                    .executes(c -> {
                      User user = getUserSender(c);
                      SignShop shop = getShop(user.getPlayer());
                      Sign sign = shop.getSign();

                      int line = c.getArgument("line_actual", Integer.class);

                      ViewerAwareMessage viewerAware = Arguments.getMessage(c, "line_text");
                      Component text = viewerAware.asComponent();

                      if (Text.isDashClear(text)) {
                        text = Component.empty();
                      }

                      sign.line(line - 1, text);
                      sign.update();

                      user.sendMessage(SMessages.setLine(user, line, text));

                      updateShop(shop);
                      return 0;
                    })
                )
            )
        );
  }

  private int setType(CommandContext<CommandSource> c, boolean sell) throws CommandSyntaxException {
    Player player = c.getSource().asPlayer();
    SignShop shop = getShop(player);

    ShopType type = shop.getType();

    ShopType to = sell ?
        (type.isAdmin() ? ShopType.ADMIN_SELL : ShopType.SELL)
        : (type.isAdmin() ? ShopType.ADMIN_BUY : ShopType.BUY);

    shop.setType(to);

    player.sendMessage(SMessages.setShopType(player, to));

    updateShop(shop);
    return 0;
  }

  private SignShop getShop(Player player) throws CommandSyntaxException {
    return getShop(manager, player);
  }

  static SignShop getShop(ShopManager manager, Player player) throws CommandSyntaxException {
    Block block = player.getTargetBlockExact(5);

    if (!SignShops.isShop(block)) {
      throw SExceptions.lookAtShop(player);
    }

    SignShop result = manager.getShop(block);

    if (!SignShops.mayEdit(result, player.getUniqueId())
        && !player.hasPermission(SPermissions.ADMIN)
    ) {
      throw SExceptions.lookAtShop(player);
    }

    return result;
  }

  private void updateShop(SignShop shop) {
    Tasks.runLater(() -> {
      shop.delayUnload();
      shop.update();
    }, 1);
  }
}