package net.arcadiusmc.factions;

import com.mojang.serialization.Codec;
import java.time.Duration;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@Accessors(chain = true)
@ConfigSerializable
public class FactionsConfig {

  static final ExistingObjectCodec<FactionsConfig> CODEC;

  private Duration joinCooldown = Duration.ofDays(2);
  private Duration leaveCooldown = Duration.ofDays(2);

  private int reputationPenalty = 100;

  private int startingReputation = 10;

  private int minReputation = -100;
  private int maxReputation = 100;

  private float minSellShopMultiplier = 1;
  private float maxSellShopMultiplier = 2;

  private String channelUrlTemplate = "https://discord.com/channels/%GUILD%/%CHANNEL%";

  void minMaxReputation() {
    int a = minReputation;
    int b = maxReputation;

    minReputation = Math.min(a, b);
    maxReputation = Math.max(a, b);
  }

  static {
    CODEC = ExistingObjectCodec.create(builder -> {
      builder.optional("faction-join-cooldown", ExtraCodecs.DURATION)
          .getter(cfg -> cfg.joinCooldown)
          .setter((cfg, duration) -> cfg.joinCooldown = duration);

      builder.optional("faction-leave-cooldown", ExtraCodecs.DURATION)
          .getter(cfg -> cfg.leaveCooldown)
          .setter((cfg, duration) -> cfg.leaveCooldown = duration);

      builder.optional("faction-leave-reputation-penalty", Codec.INT)
          .getter(config -> config.reputationPenalty)
          .setter((config, integer) -> config.reputationPenalty = integer);

      builder.optional("max-reputation", Codec.INT)
          .getter(config -> config.maxReputation)
          .setter((config, integer) -> config.maxReputation = integer);

      builder.optional("min-reputation", Codec.INT)
          .getter(config -> config.minReputation)
          .setter((config, integer) -> config.minReputation = integer);

      builder.optional("channel-url-template", Codec.STRING)
          .getter(config -> config.channelUrlTemplate)
          .setter((config, s) -> config.channelUrlTemplate = s);

      builder.optional("starting-reputation", Codec.INT)
          .getter(config -> config.startingReputation)
          .setter((config, integer) -> config.startingReputation = integer);

      builder.optional("min-sellshop-multiplier", Codec.FLOAT)
          .getter(config -> config.minSellShopMultiplier)
          .setter((config, integer) -> config.minSellShopMultiplier = integer);

      builder.optional("max-sellshop-multiplier", Codec.FLOAT)
          .getter(config -> config.maxSellShopMultiplier)
          .setter((config, integer) -> config.maxSellShopMultiplier = integer);
    });
  }
}
