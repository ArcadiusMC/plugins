package net.arcadiusmc.afk;

import static net.arcadiusmc.afk.Afk.TICK_INTERVAL;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.afk.AfkConfig.AfkPunishment;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.channel.MessageHandler;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.Time;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Getter
public class PlayerAfkState {

  private static final Logger LOGGER = Loggers.getLogger();

  private final UUID playerId;

  private boolean afk;
  private ViewerAwareMessage reason;

  @Setter
  private int afkTicks;

  public PlayerAfkState(UUID playerId) {
    this.playerId = playerId;
  }

  public void afk(ViewerAwareMessage reason) {
    User user = Users.get(playerId);

    user.ensureOnline();
    Preconditions.checkState(!afk, "User is already AFK");

    afkTicks = 0;
    setState(user, true, reason);
    afkAnnouncement(user, reason);
  }

  public void unafk() {
    User user = Users.get(playerId);

    user.ensureOnline();
    Preconditions.checkState(afk, "User is not AFK");

    afkTicks = 0;
    setState(user, false, null);

    ChannelledMessage.announce(viewer -> {
      String messageKey = "afk.unafk." + (Audiences.equals(user, viewer) ? "self" : "other");

      return Messages.render(messageKey)
          .addValue("player", user)
          .create(viewer);
    });
  }

  private void setState(User user, boolean state, ViewerAwareMessage reason) {
    if (state) {
      if (!afk) {
        user.setTimeToNow(TimeField.AFK_START);
      }

      this.reason = reason;
    } else {
      if (afk) {
        logAfkTime(user);
      }

      this.reason = null;
    }

    afk = state;
    user.updateTabName();
  }

  public void logAfkTime(User user) {
    long startTime = user.getTime(TimeField.AFK_START);

    if (startTime != -1) {
      long since = Time.timeSince(startTime);
      long currentValue = user.getTime(TimeField.AFK_TIME);
      user.setTime(TimeField.AFK_TIME, currentValue == -1 ? since : currentValue + since);
    }
  }

  void tick(AfkConfig config) {
    afkTicks += TICK_INTERVAL;

    long millis = Time.ticksToMillis(afkTicks);

    Duration autoAfkDelay = config.getAutoAfkDelay();
    Duration maxAfkTime = config.getAllowedAfkTime();
    Duration untilPunish = autoAfkDelay.plus(maxAfkTime);

    if (millis >= untilPunish.toMillis()) {
      if (config.getAllowedTimeSurpassed() == AfkPunishment.NONE) {
        return;
      }

      punish();
      return;
    } else if (afk) {
      return;
    }

    if (millis < autoAfkDelay.toMillis()) {
      return;
    }

    autoAfk();
  }

  private void punish() {
    User user = Users.get(playerId);

    if (!user.isOnline() || !afk) {
      return;
    }

    AfkConfig config = AfkPlugin.plugin().getAfkConfig();
    AfkPunishment punishment = config.getAllowedTimeSurpassed();

    if (punishment == null || punishment == AfkPunishment.NONE) {
      return;
    }

    punishment.punish(user, config);
  }

  private void autoAfk() {
    User user = Users.get(playerId);

    if (!user.isOnline() || afk) {
      return;
    }

    AfkConfig config = AfkPlugin.plugin().getAfkConfig();

    MessageRender reason = Messages.render("afk.autoAfkReason")
        .addValue("time", config.getAutoAfkDelay())
        .addValue("player", user);

    LOGGER.debug("Setting auto afk");

    setState(user, true, reason);
    afkAnnouncement(user, reason);
  }

  private void afkAnnouncement(User user, ViewerAwareMessage message) {
    ViewerAwareMessage nonNullReason = message == null
        ? ViewerAwareMessage.wrap(Component.empty())
        : message;

    ChannelledMessage channelled = ChannelledMessage.create(nonNullReason)
        .setSource(user)
        .setBroadcast()
        .setChannelName("afk");

    channelled.setRenderer((viewer, baseMessage) -> {
      String messageKey = "afk."
          + (Audiences.equals(viewer, user) ? "self" : "other")
          + "."
          + (Text.isEmpty(baseMessage) ? "noMessage" : "message");

      return Messages.render(messageKey)
          .addValue("player", user)
          .addValue("message", baseMessage)
          .create(viewer);
    });

    channelled.setHandler(MessageHandler.EMPTY_IF_VIEWER_WAS_REMOVED);
    channelled.send();
  }
}
