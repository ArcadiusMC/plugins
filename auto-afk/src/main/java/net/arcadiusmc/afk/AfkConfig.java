package net.arcadiusmc.afk;

import java.time.Duration;
import java.time.ZonedDateTime;
import lombok.Getter;
import net.arcadiusmc.user.User;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class AfkConfig {

  private boolean autoAFkEnabled = true;
  private Duration autoAfkDelay = Duration.ofMinutes(10);
  private Duration allowedAfkTime = Duration.ofHours(1);
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
        // TODO
      }
    },

    TEMPBAN {
      @Override
      public void punish(User user, AfkConfig config) {
        ZonedDateTime now = ZonedDateTime.now();

        ZonedDateTime time = ZonedDateTime.now()
            .withSecond(0)
            .withHour(0)
            .withMinute(0)
            .plusDays(1);

        Duration between = Duration.between(now, time);

        // TODO (After the admin plugin has been added)
      }
    },

    PERMBAN {
      @Override
      public void punish(User user, AfkConfig config) {
        // TODO
      }
    };
    
    public abstract void punish(User user, AfkConfig config);
  }
}
