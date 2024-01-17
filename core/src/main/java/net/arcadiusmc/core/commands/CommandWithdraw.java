package net.arcadiusmc.core.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nonnegative;
import net.arcadiusmc.core.Coins;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;
import org.bukkit.Sound;

public class CommandWithdraw extends BaseCommand {

  public CommandWithdraw() {
    super("withdraw");

    setDescription("Get cold coins from your balance");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<amount>")
        .addInfo("Withdraws a coin worth <amount>");

    factory.usage("<amount> <coins>")
        .addInfo("Withdraws <coins>, each worth <amount>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("amount", Arguments.MONEY)

        .then(argument("coinAmount",
            IntegerArgumentType.integer(1, Coins.COIN_MATERIAL.getMaxStackSize()))
            .executes(c -> giveCoins(c, c.getArgument("coinAmount", Integer.class)))
        )

        .executes(c -> giveCoins(c, 1))
    );
  }

  private int giveCoins(CommandContext<CommandSource> c, @Nonnegative int itemAmount)
      throws CommandSyntaxException
  {
    User user = getUserSender(c);
    int amount = c.getArgument("amount", Integer.class);
    int totalAmount = amount * itemAmount;

    if (!user.hasBalance(totalAmount)) {
      throw Exceptions.cannotAfford(totalAmount);
    }

    var inventory = user.getPlayer().getInventory();

    if (inventory.firstEmpty() == -1) {
      throw Exceptions.INVENTORY_FULL;
    }

    inventory.addItem(Coins.makeCoins(amount, itemAmount, user));

    user.removeBalance(totalAmount);
    user.sendMessage(CoreMessages.withdrew(itemAmount, totalAmount));
    user.playSound(Sound.ENTITY_ITEM_PICKUP, 1, 1);

    return 0;
  }
}