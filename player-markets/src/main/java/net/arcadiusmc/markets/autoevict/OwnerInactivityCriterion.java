package net.arcadiusmc.markets.autoevict;

import com.mojang.brigadier.arguments.ArgumentType;
import java.time.Duration;
import java.time.Instant;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.TimeArgument;
import net.kyori.adventure.text.Component;

public class OwnerInactivityCriterion extends CriterionType<Duration> {

  static final String KEY = "max-offline-time";

  public OwnerInactivityCriterion() {
    super(ExtraCodecs.DURATION);
  }

  @Override
  public boolean test(Duration value, MarketScanResult result) {
    long lastLogin = result.getLastOwnerLogin();

    Instant loginInstant = Instant.ofEpochMilli(lastLogin);
    Instant scanInstant = Instant.ofEpochMilli(result.getScanTimestamp());

    return isGreaterThan(value, loginInstant, scanInstant);
  }

  static boolean isGreaterThan(Duration value, Instant min, Instant max) {
    Duration interval = Duration.between(min, max);
    return value.toMillis() > interval.toMillis();
  }

  @Override
  public Component getReasonDisplay(Duration value) {
    return Messages.render("markets.evictionReasons", KEY)
        .addValue("time", value)
        .asComponent();
  }

  @Override
  public String toString(Duration value) {
    return TimeArgument.toString(value);
  }

  @Override
  protected ArgumentType<Duration> argumentType() {
    return ArgumentTypes.time();
  }
}
