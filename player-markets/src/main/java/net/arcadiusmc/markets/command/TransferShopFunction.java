package net.arcadiusmc.markets.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.Markets;
import net.arcadiusmc.markets.command.MarketTargetCommand.TargetFunction;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.event.ClickEvent;

class TransferShopFunction implements TargetFunction {

  static final long CONFIRM_AWAIT_TIME = TimeUnit.MINUTES.toMillis(2);

  // PlayerID to initial command time
  private final Object2LongMap<UUID> awaitingConfirmation = new Object2LongOpenHashMap<>();

  @Override
  public void run(User user, User target, Market market) throws CommandSyntaxException {
    long initialTime = awaitingConfirmation.getLong(user.getUniqueId());
    long current = System.currentTimeMillis();

    if (current <= initialTime + CONFIRM_AWAIT_TIME) {
      transfer(user, target, market);
      awaitingConfirmation.removeLong(user.getUniqueId());
      return;
    }

    awaitingConfirmation.put(user.getUniqueId(), current);

    user.sendMessage(
        Messages.render("markets.transfer.needsConfirmation")
            .addValue("sender", user)
            .addValue("target", target)
            .addValue("confirm",
                Messages.render("markets.transfer.confirm").create(user)
                    .clickEvent(ClickEvent.runCommand("/transfershop " + target.getName()))
            )
            .create(user)
    );
  }

  private void transfer(User sender, User target, Market market) throws CommandSyntaxException {
    Markets.validateActionCooldown(sender);
    market.transfer(target);

    sender.sendMessage(
        Messages.render("markets.transfer.done.sender")
            .addValue("sender", sender)
            .addValue("target", target)
            .create(sender)
    );

    target.sendMessage(
        Messages.render("markets.transfer.done.target")
            .addValue("sender", sender)
            .addValue("target", target)
            .create(target)
    );
  }
}
