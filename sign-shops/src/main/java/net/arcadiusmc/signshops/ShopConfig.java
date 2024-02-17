package net.arcadiusmc.signshops;

import java.time.Duration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter @Accessors(fluent = true)
@ConfigSerializable
public class ShopConfig {
  private boolean logAdminUses = true;
  private boolean logNormalUses = false;
  private int maxPrice = 1000000;
  private Duration unloadDelay = Duration.ofMinutes(5);
}
