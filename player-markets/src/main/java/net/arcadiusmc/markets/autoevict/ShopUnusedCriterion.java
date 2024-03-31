package net.arcadiusmc.markets.autoevict;

import static net.arcadiusmc.markets.autoevict.OwnerInactivityCriterion.isGreaterThan;

import com.mojang.brigadier.arguments.ArgumentType;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;
import net.arcadiusmc.markets.autoevict.MarketScanResult.SignShopBlock;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.TimeArgument;
import net.kyori.adventure.text.Component;
import org.spongepowered.math.GenericMath;

public class ShopUnusedCriterion extends CriterionType<Duration> {

  static final String KEY = "average-shop-unused-time";

  public ShopUnusedCriterion() {
    super(ExtraCodecs.DURATION);
  }

  @Override
  public boolean test(Duration value, MarketScanResult result) {
    OptionalDouble averageOpt = result.getBlocks()
        .stream()
        .mapToLong(SignShopBlock::lastUseTimestamp)
        .average();

    if (averageOpt.isEmpty()) {
      return false;
    }

    return isGreaterThan(
        value,
        Instant.ofEpochMilli(GenericMath.floorl(averageOpt.getAsDouble())),
        Instant.ofEpochMilli(result.getScanTimestamp())
    );
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
