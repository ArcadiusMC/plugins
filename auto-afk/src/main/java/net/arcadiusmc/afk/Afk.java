package net.arcadiusmc.afk;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.channel.MessageHandler;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.Time;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public final class Afk {

  private static final Map<UUID, AfkState> stateMap = new Object2ObjectOpenHashMap<>();

  private static Optional<AfkState> getState(User user) {
    return Optional.ofNullable(stateMap.get(user.getUniqueId()));
  }

  public static boolean isAfk(User user) {
    return getState(user).map(afkState -> afkState.state).orElse(false);
  }

  public static Optional<PlayerMessage> getAfkReason(User user) {
    return getState(user).map(AfkState::getReason);
  }

  public static void setAfk(User user, boolean afk, PlayerMessage reason) {
    AfkState state = stateMap.computeIfAbsent(user.getUniqueId(), uuid -> new AfkState());

    if (afk) {
      if (!state.state) {
        user.setTimeToNow(TimeField.AFK_START);
      }

      state.reason = reason;
    } else {
      if (state.state) {
        logAfkTime(user);
      }

      state.reason = null;
    }

    state.state = afk;
    user.updateTabName();
  }

  private static void logAfkTime(User user) {
    long startTime = user.getTime(TimeField.AFK_START);

    if (startTime != -1) {
      long since = Time.timeSince(startTime);
      long currentValue = user.getTime(TimeField.AFK_TIME);
      user.setTime(TimeField.AFK_TIME, currentValue == -1 ? since : currentValue + since);
    }
  }

  public static void afk(User user, @Nullable PlayerMessage reason) {
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

  @Getter
  private static class AfkState {
    boolean state;
    PlayerMessage reason;
  }
}
