package net.arcadiusmc.markets.autoevict;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.serialization.Codec;
import java.util.List;
import net.arcadiusmc.markets.autoevict.MarketScanResult.SignShopBlock;
import net.arcadiusmc.text.Messages;
import net.kyori.adventure.text.Component;
import org.spongepowered.math.GenericMath;

public class ShopStockCriterion extends CriterionType<Float> {

  static final String KEY = "average-stock-rate";

  public ShopStockCriterion() {
    super(Codec.FLOAT);
  }

  @Override
  public boolean test(Float value, MarketScanResult result) {
    List<SignShopBlock> blocks = result.getBlocks();
    int total = blocks.size();
    int stocked = 0;

    for (SignShopBlock block : blocks) {
      if (!block.type().isBuyType()) {
        continue;
      }

      if (!block.isStocked()) {
        continue;
      }

      stocked++;
    }

    float stockRatio = (float) stocked / total;
    return stockRatio >= value;
  }

  @Override
  public Component getReasonDisplay(Float value) {
    int percent = GenericMath.floor(value * 100);

    return Messages.render("markets.evictionReasons", KEY)
        .addValue("percent", percent)
        .asComponent();
  }

  @Override
  public String toString(Float value) {
    return value.toString();
  }

  @Override
  protected ArgumentType<Float> argumentType() {
    return FloatArgumentType.floatArg();
  }
}
