package net.arcadiusmc.punish;

import com.google.common.base.Strings;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class Punishments {
  private Punishments() {}

  public static PunishPlugin getPlugin() {
    return PunishPlugin.plugin();
  }

  public static JailManager getJails() {
    return getPlugin().getJails();
  }

  public static PunishManager getManager() {
    return getPlugin().getPunishManager();
  }

  public static PunishEntry entry(User user) {
    return getManager().getEntry(user.getUniqueId());
  }

  public static Optional<PunishEntry> getOptionalEntry(User user) {
    return getManager().getOptionalEntry(user.getUniqueId());
  }

  public static MuteState checkMute(Audience audience) {
    MuteState state = getMute(audience);
    User user = Audiences.getUser(audience);

    if (state != MuteState.HARD || user == null) {
      return state;
    }

    PunishEntry entry = entry(user);
    Punishment punishment = entry.getCurrent(PunishType.MUTE);

    assert punishment != null;

    String reason = punishment.getReason();

    MessageRender render;

    if (Strings.isNullOrEmpty(reason)) {
      render = Messages.render("muted.generic");
    } else {
      render = Messages.render("muted.withReason")
          .addValue("reason", Text.valueOf(reason));
    }

    audience.sendMessage(render.create(audience));
    return state;
  }

  public static MuteState getMute(Audience audience) {
    User user = Audiences.getUser(audience);

    if (user == null) {
      return MuteState.NONE;
    }

    return getMute(user);
  }

  public static MuteState getMute(User user) {
    return getOptionalEntry(user)
        .map(entry -> {
          if (entry.isPunished(PunishType.SOFTMUTE)) {
            return MuteState.SOFT;
          }
          if (entry.isPunished(PunishType.MUTE)) {
            return MuteState.HARD;
          }
          return MuteState.NONE;
        })

        .orElse(MuteState.NONE);
  }

  /**
   * Checks if the source can punish the given user
   *
   * @param source The source attempting to punish
   * @param user   The user to punish
   *
   * @return True, if the source is either OP or has {@link GPermissions#ADMIN} permissions OR the
   * target does not have those permissions
   */
  public static boolean canPunish(CommandSource source, User user) {
    if (source.hasPermission(GPermissions.ADMIN)) {
      return true;
    }

    return !user.getOfflinePlayer().isOp() && !user.hasPermission(GPermissions.ADMIN);
  }

  public static void punish(
      CommandSource source,
      User target,
      PunishType type,
      String reason,
      String extra,
      Duration length
  ) {
    Instant now = Instant.now();
    Instant expires = length == null ? null : now.plus(length);

    Punishment punishment = new Punishment(type, source.textName(), reason, extra, now, expires);

    PunishEntry entry = entry(target);
    entry.punish(punishment, source);
  }

  public static void announce(ViewerAwareMessage message, @Nullable Audience source) {
    ChannelledMessage ch = ChannelledMessage.create(message);
    ch.setBroadcast();

    ch.filterTargets(audience -> {
      Player player = Audiences.getPlayer(audience);

      if (player == null) {
        return true;
      }

      return player.hasPermission(GPermissions.BROADCASTS);
    });

    MessageRender baseFormat;

    if (source == null) {
      baseFormat = Messages.render("punishments.broadcast");
    } else {
      baseFormat = Messages.render("punishments.broadcast.player")
          .addValue("source", source);
    }

    ch.setRenderer((viewer, baseMessage) -> {
      if (Audiences.equals(viewer, source)) {
        return baseMessage;
      }

      return baseFormat
          .addValue("message", baseMessage)
          .create(viewer);
    });

    ch.send();
  }
}
