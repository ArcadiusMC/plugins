package net.arcadiusmc.markets.autoevict;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.arcadiusmc.text.Messages;
import net.kyori.adventure.text.Component;

public class ShopCountCriterion extends CriterionType<Integer> {

  static final String KEY = "shop-count";

  public ShopCountCriterion() {
    super(Codec.INT);
  }

  @Override
  public boolean test(Integer value, MarketScanResult result) {
    return result.getBlocks().size() >= value;
  }

  @Override
  public Component getReasonDisplay(Integer value) {
    return Messages.render("markets.evictionReason", KEY)
        .addValue("amount", value)
        .asComponent();
  }

  @Override
  public String toString(Integer value) {
    return value.toString();
  }

  @Override
  protected ArgumentType<Integer> argumentType() {
    return IntegerArgumentType.integer();
  }
}
