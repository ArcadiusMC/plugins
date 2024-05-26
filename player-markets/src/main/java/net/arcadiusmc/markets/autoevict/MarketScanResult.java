package net.arcadiusmc.markets.autoevict;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.signshops.ShopType;
import net.arcadiusmc.utils.io.ExtraCodecs;

@RequiredArgsConstructor
public class MarketScanResult {

  public static final Codec<MarketScanResult> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            SignShopBlock.CODEC.listOf()
                .optionalFieldOf("blocks", List.of())
                .forGetter(o -> o.blocks),

            Codec.LONG.fieldOf("last_owner_login")
                .forGetter(o -> o.lastOwnerLogin),

            Codec.FLOAT.fieldOf("scan_aggression")
                .forGetter(o -> o.scanAggression),

            Codec.LONG.fieldOf("scan_timestamp")
                .forGetter(o -> o.scanTimestamp)
        )
        .apply(instance, MarketScanResult::new);
  });

  private final List<SignShopBlock> blocks;

  @Getter
  private final long lastOwnerLogin;

  @Getter
  private final float scanAggression;

  @Getter
  private final long scanTimestamp;

  public List<SignShopBlock> getBlocks() {
    return Collections.unmodifiableList(blocks);
  }

  public Stream<SignShopBlock> blocks() {
    return blocks.stream();
  }

  public record SignShopBlock(
      int stackSize,
      int stockAmount,
      int exampleItemAmount,
      int inventorySize,
      long lastUseTimestamp,
      ShopType type
  ) {
    public static final Codec<SignShopBlock> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.INT.fieldOf("stack_size").forGetter(o -> o.stackSize),
              Codec.INT.fieldOf("stock_amount").forGetter(o -> o.stockAmount),
              Codec.INT.fieldOf("example_item_size").forGetter(o -> o.exampleItemAmount),
              Codec.INT.fieldOf("inventory_size").forGetter(o -> o.inventorySize),
              Codec.LONG.fieldOf("last_use_timestamp").forGetter(o -> o.lastUseTimestamp),
              ExtraCodecs.enumCodec(ShopType.class).fieldOf("shop_type").forGetter(o -> o.type)
          )
          .apply(instance, SignShopBlock::new);
    });

    public boolean isStocked() {
      if (type.isBuyType()) {
        return stockAmount >= exampleItemAmount;
      }

      int totalItemSize = inventorySize * stackSize;
      return stockAmount + exampleItemAmount <= totalItemSize;
    }
  }
}
