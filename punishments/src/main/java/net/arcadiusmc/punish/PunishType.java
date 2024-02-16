package net.arcadiusmc.punish;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import org.bukkit.BanEntry;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent.Cause;
import org.bukkit.permissions.Permission;
import org.slf4j.Logger;

@Getter
public enum PunishType {
  MUTE ("mute"),
  SOFTMUTE ("softmute"),

  JAIL ("jail") {
    @Override
    public String getPunishAnnounceFormat(boolean hasReason) {
      return "punishments.jailed" + (hasReason ? ".reason" : "");
    }
  },

  KICK ("kick") {
    @Override
    public void onPunishmentBegin(User user, Punishment punishment) {
      if (!user.isOnline()) {
        return;
      }

      String reason = punishment.getReason();
      Player player = user.getPlayer();

      if (Strings.isNullOrEmpty(reason)) {
        player.kick(Component.text("Kicked from the server!"), Cause.KICK_COMMAND);
      } else {
        Component text = Placeholders.renderString(reason, user);
        player.kick(text, Cause.KICK_COMMAND);
      }
    }
  },

  BAN ("ban") {
    private static final Logger LOGGER = Loggers.getLogger();

    @Override
    public void onPunishmentBegin(User user, Punishment punishment) {
      ProfileBanList list = Bukkit.getBanList(Type.PROFILE);
      PlayerProfile profile = user.getProfile();

      BanEntry<PlayerProfile> entry = list.addBan(
          profile,
          punishment.getReason(),
          punishment.getExpires(),
          punishment.getSource()
      );

      if (entry == null) {
        LOGGER.error("Failed to create ban entry for player {}, punishment={}", user, punishment);
      }

      KICK.onPunishmentBegin(user, punishment);
    }

    @Override
    public void onPunishmentEnd(User user, Punishment punishment) {
      ProfileBanList list = Bukkit.getBanList(Type.PROFILE);
      PlayerProfile profile = user.getProfile();
      list.pardon(profile);
    }
  },

  IPBAN ("ipban") {
    private static final Logger LOGGER = Loggers.getLogger();

    Optional<InetAddress> getAddress(User user) {
      try {
        return Optional.of(InetAddress.getByName(user.getIp()));
      } catch (UnknownHostException e) {
        LOGGER.error("Error getting IP address of player {}", user, e);
        return Optional.empty();
      }
    }

    @Override
    public void onPunishmentBegin(User user, Punishment punishment) {
      IpBanList list = Bukkit.getBanList(Type.IP);

      BanEntry<InetAddress> entry = getAddress(user)
          .map(inetAddress -> {
            return list.addBan(
                inetAddress,
                punishment.getReason(),
                punishment.getExpires(),
                punishment.getSource()
            );
          })
          .orElse(null);

      if (entry == null) {
        LOGGER.error("Failed to create ban entry for player {}, punishment={}", user, punishment);
      }

      KICK.onPunishmentBegin(user, punishment);
    }

    @Override
    public void onPunishmentEnd(User user, Punishment punishment) {
      IpBanList list = Bukkit.getBanList(Type.IP);
      getAddress(user).ifPresent(list::pardon);
    }
  };

  public static final Codec<PunishType> CODEC = ExtraCodecs.enumCodec(PunishType.class);

  private final Permission permission;
  private final String id;

  PunishType(String id) {
    this.id = id;
    this.permission = Permissions.register("arcadius." + id);
  }

  public Component namedEndingEd() {
    return Messages.render("punishTypes", id, "endingEd").asComponent();
  }

  public Component presentableName() {
    return Messages.render("punishTypes", id).asComponent();
  }

  public String getPunishAnnounceFormat(boolean hasReason) {
    return "punishments.generic" + (hasReason ? ".reason" : "");
  }

  public void onPunishmentBegin(User user, Punishment punishment) {

  }

  public void onPunishmentEnd(User user, Punishment punishment) {

  }
}
