package net.arcadiusmc.markets.command;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.command.SimpleMarketCommand.CommandFunction;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.event.ClickEvent;

public class UnclaimShopFunction implements CommandFunction {

  static final long CONFIRM_AWAIT_TIME = TimeUnit.MINUTES.toMillis(2);

  // PlayerID to initial command time
  private final Object2LongMap<UUID> awaitingConfirmation = new Object2LongOpenHashMap<>();

  @Override
  public void run(User user, Market market) {
    long initialTime = awaitingConfirmation.getLong(user.getUniqueId());
    long current = System.currentTimeMillis();

    if (current < initialTime + CONFIRM_AWAIT_TIME) {
      market.unclaim();
      awaitingConfirmation.removeLong(user.getUniqueId());

      market.getManager().getPlugin().getResets().reset(market);

      user.sendMessage(Messages.renderText("markets.unclaim.done", user));
      return;
    }

    awaitingConfirmation.put(user.getUniqueId(), current);

    user.sendMessage(
        Messages.render("markets.unclaim.needsConfirmation")
            .addValue("confirm",
                Messages.renderText("markets.unclaim.confirm", user)
                    .clickEvent(ClickEvent.runCommand("/unclaimshop"))
            )
    );
  }
}
