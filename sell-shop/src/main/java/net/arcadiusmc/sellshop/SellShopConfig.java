package net.arcadiusmc.sellshop;

import java.time.Duration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
@Accessors(fluent = true)
public class SellShopConfig {

  private int defaultMaxEarnings = 500000;

  private Duration earningLossInterval = Duration.ofDays(1);

  private int earningLoss = 5000;

  private boolean logSelling = true;

  private boolean logAutoSellOnlyWhenEnds = true;

  private String mainPageName = "main";
}
