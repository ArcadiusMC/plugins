package net.arcadiusmc.afk;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
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
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public final class Afk {

  private static final Map<UUID, AfkState> stateMap = new Object2ObjectOpenHashMap<>();

  public static Optional<AfkState> getState(User user) {
    return Optional.ofNullable(stateMap.get(user.getUniqueId()));
  }

  public static boolean isAfk(User user) {
    return getState(user).map(afkState -> afkState.afk).orElse(false);
  }

  public static Optional<ViewerAwareMessage> getAfkReason(User user) {
    return getState(user).map(AfkState::getReason);
  }

  public static AfkState getOrCreateEntry(User user) {
    return stateMap.computeIfAbsent(user.getUniqueId(), AfkState::new);
  }

  public static void setAfk(User user, boolean afk, ViewerAwareMessage reason) {
    AfkState state = getOrCreateEntry(user);

    if (afk) {
      if (!state.afk) {
        user.setTimeToNow(TimeField.AFK_START);
      }

      state.reason = reason;

      state.cancelAutoAfk();
      state.schedulePunishTask();
    } else {
      if (state.afk) {
        logAfkTime(user);
      }

      state.reason = null;

      state.cancelPunishTask();
      state.scheduleAutoAfk();
    }

    state.afk = afk;
    user.updateTabName();
  }

  public static void logAfkTime(User user) {
    long startTime = user.getTime(TimeField.AFK_START);

    if (startTime != -1) {
      long since = Time.timeSince(startTime);
      long currentValue = user.getTime(TimeField.AFK_TIME);
      user.setTime(TimeField.AFK_TIME, currentValue == -1 ? since : currentValue + since);
    }
  }

  public static void afk(User user, @Nullable ViewerAwareMessage reason) {
    user.ensureOnline();
    Preconditions.checkState(!isAfk(user), "User is already AFK");

    setAfk(user, true, reason);

    ViewerAwareMessage nonNullReason = reason == null
        ? ViewerAwareMessage.wrap(Component.empty())
        : reason;

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

  public static void unafk(User user) {
    user.ensureOnline();
    Preconditions.checkState(isAfk(user), "User is not AFK");

    setAfk(user, false, null);

    ChannelledMessage.announce(viewer -> {
      String messageKey = "afk.unafk." + (Audiences.equals(user, viewer) ? "self" : "other");

      return Messages.render(messageKey)
          .addValue("player", user)
          .create(viewer);
    });
  }

  public static void delayAutoAfk(User user) {
    getState(user).ifPresent(afkState -> {
      if (afkState.afk) {
        return;
      }

      afkState.scheduleAutoAfk();
    });
  }

  @Getter
  public static class AfkState {
    final UUID playerId;

    boolean afk;
    ViewerAwareMessage reason;

    BukkitTask autoAfkTask;
    BukkitTask autoPunishTask;

    public AfkState(UUID playerId) {
      this.playerId = playerId;
    }

    public void cancelPunishTask() {
      autoPunishTask = Tasks.cancel(autoPunishTask);
    }

    public void schedulePunishTask() {
      cancelPunishTask();

      AfkConfig config = AfkPlugin.plugin().getAfkConfig();

      if (!config.isAutoAFkEnabled()) {
        return;
      }

      Duration untilPunish = config.getAllowedAfkTime();
      autoPunishTask = Tasks.runLater(this::punish, untilPunish);
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

    public void cancelAutoAfk() {
      autoAfkTask = Tasks.cancel(autoAfkTask);
    }

    public void scheduleAutoAfk() {
      cancelAutoAfk();

      AfkConfig config = AfkPlugin.plugin().getAfkConfig();

      if (!config.isAutoAFkEnabled()) {
        return;
      }

      Duration untilAfk = config.getAutoAfkDelay();

      autoAfkTask = Tasks.runLater(this::autoAfk, untilAfk);
    }

    private void autoAfk() {
      User user = Users.get(playerId);

      if (user.isOnline() || !afk) {
        return;
      }

      AfkConfig config = AfkPlugin.plugin().getAfkConfig();

      MessageRender reason = Messages.render("afk.autoAfkReason")
          .addValue("time", config.getAutoAfkDelay())
          .addValue("player", user);

      afk(user, reason);
    }
  }
}
