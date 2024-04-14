package net.arcadiusmc.markets;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.time.Instant;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.Component;

public final class Markets {
  private Markets() {}

  public static void validateActionCooldown(User user) throws CommandSyntaxException {
    Component msg = actionCooldown(user);

    if (msg == null) {
      return;
    }

    throw Exceptions.create(msg);
  }

  public static boolean testActionCooldown(User user) {
    return actionCooldown(user) == null;
  }

  public static Component actionCooldown(User user) {
    MarketsPlugin plugin = MarketsPlugin.plugin();
    MarketsConfig config = plugin.getPluginConfig();

    Duration interval = config.actionCooldown();

    Instant lastAction = Instant.ofEpochMilli(user.getTime(TimeField.MARKET_LAST_ACTION));
    Instant now = Instant.now();
    Instant nextAllowed = lastAction.plus(interval);

    if (now.isBefore(nextAllowed)) {
      Duration remaining = Duration.between(now, nextAllowed);
      return Messages.cooldownMessage(user, remaining, interval);
    }

    return null;
  }

  public static MarketsManager getManager() {
    return MarketsPlugin.plugin().getManager();
  }

  public static Market getOwned(User user) {
    return getManager().getByOwner(user.getUniqueId());
  }
}
