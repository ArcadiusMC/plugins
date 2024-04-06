package net.arcadiusmc.afk;

import java.time.Duration;
import java.time.ZonedDateTime;
import lombok.Getter;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.Grenadier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class AfkConfig {

  private boolean autoAfkEnabled = true;

  private Duration autoAfkDelay = Duration.ofMinutes(10);
  private Duration allowedAfkTime = Duration.ofHours(1);
  private Duration minTempbanTime = Duration.ofHours(4);

  private AfkPunishment allowedTimeSurpassed = AfkPunishment.NONE;
  
  public enum AfkPunishment {
    NONE {
      @Override
      public void punish(User user, AfkConfig config) {
        // No-op
      }
    },

    KICK {
      @Override
      public void punish(User user, AfkConfig config) {
        punish(user, config, PunishType.KICK, null);
      }
    },

    TEMPBAN {
      @Override
      public void punish(User user, AfkConfig config) {
        ZonedDateTime now = ZonedDateTime.now();

        ZonedDateTime time = now
            .withSecond(0)
            .withHour(0)
            .withMinute(0)
            .plusDays(1);

        Duration between = Duration.between(now, time);
        Duration punishLength;

        if (between.compareTo(config.minTempbanTime) < 0) {
          punishLength = config.minTempbanTime;
        } else {
          punishLength = between;
        }

        punish(user, config, PunishType.BAN, punishLength);
      }
    },

    PERMBAN {
      @Override
      public void punish(User user, AfkConfig config) {
        punish(user, config, PunishType.BAN, null);
      }
    };
    
    public abstract void punish(User user, AfkConfig config);

    protected void punish(User user, AfkConfig config, PunishType type, Duration duration) {
      Punishments.punish(
          Grenadier.createSource(Bukkit.getConsoleSender()),
          user,
          type,
          punishReason(config),
          null,
          duration
      );
    }

    private String punishReason(AfkConfig config) {
      Duration maxAfkTime = config.allowedAfkTime;

      Component message = Messages.render("afk.punishmentReason")
          .addValue("maxAfkTime", maxAfkTime)
          .asComponent();

      return Text.plain(message);
    }
  }
}
