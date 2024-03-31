package net.arcadiusmc.markets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

@Getter
public class Eviction implements Runnable {

  private static final Logger LOGGER = Loggers.getLogger();

  public static final Codec<Eviction> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("market_name").forGetter(o -> o.marketName),
            Codec.STRING.fieldOf("source").forGetter(o -> o.source),

            ExtraCodecs.COMPONENT.optionalFieldOf("reason", Component.empty())
                .forGetter(o -> o.reason),

            ExtraCodecs.INSTANT.fieldOf("start").forGetter(o -> o.start),
            ExtraCodecs.INSTANT.fieldOf("date").forGetter(o -> o.date)
        )
        .apply(instance, Eviction::new);
  });

  private final String marketName;
  private final String source;

  private final Component reason;

  private final Instant start;
  private final Instant date;

  @Getter(AccessLevel.PRIVATE)
  BukkitTask task;

  @Getter(AccessLevel.PRIVATE)
  MarketsManager manager;

  public Eviction(String marketName, String source, Component reason, Instant start, Instant date) {
    Objects.requireNonNull(marketName, "Null market name");
    Objects.requireNonNull(source, "Null source");
    Objects.requireNonNull(start, "Null start");
    Objects.requireNonNull(date, "Null date");
    Objects.requireNonNull(reason, "Null reason");

    this.marketName = marketName;
    this.source = source;

    this.reason = reason;

    this.start = start;
    this.date = date;
  }

  Duration getDelay(Instant now) {
    return Duration.between(date, now);
  }

  @Override
  public void run() {
    if (manager == null) {
      return;
    }

    manager.removeEviction(this);

    Market market = manager.getMarket(marketName);

    if (market == null) {
      LOGGER.error("Cannot evict shop named '{}': Shop not found", marketName);
      return;
    }

    if (market.getOwnerId() == null) {
      LOGGER.error("Cannot evict shop named '{}': No owner", marketName);
      return;
    }

    market.unclaim();
    manager.getPlugin().getResets().reset(market);
  }

  void cancel() {
    this.task = Tasks.cancel(task);
  }
}
