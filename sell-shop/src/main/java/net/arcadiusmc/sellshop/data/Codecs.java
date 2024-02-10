package net.arcadiusmc.sellshop.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.arcadiusmc.sellshop.SellShopPlugin;
import net.arcadiusmc.sellshop.data.ItemSellData.Builder;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.Registry;

class Codecs {


  public static final Codec<ItemSellData> SELL_DATA = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.registryCodec(Registry.MATERIAL)
                .fieldOf("material")
                .forGetter(ItemSellData::getMaterial),

            Codec.INT
                .fieldOf("price")
                .forGetter(ItemSellData::getPrice),

            ExtraCodecs.registryCodec(Registry.MATERIAL)
                .optionalFieldOf("compact-material")
                .forGetter(d -> Optional.ofNullable(d.getCompactMaterial())),

            Codec.INT
                .optionalFieldOf("compact-multiplier", 9)
                .forGetter(ItemSellData::getCompactMultiplier),

            Codec.INT
                .optionalFieldOf("max-earnings")
                .forGetter(d -> Optional.of(d.getMaxEarnings()))
        )

        .apply(instance, (material, price, compactMaterial, compactMod, maxEarnings) -> {
          Builder builder = ItemSellData.builder();
          builder.material(material);
          builder.price(price);

          compactMaterial.ifPresent(compact -> {
            builder.compactMaterial(compact);
            builder.compactMultiplier(compactMod);
          });

          maxEarnings.ifPresentOrElse(builder::maxEarnings, () -> {
            SellShopPlugin plugin = SellShopPlugin.getPlugin();
            int max = plugin.getShopConfig().defaultMaxEarnings();
            builder.maxEarnings(max);
          });

          return builder.build();
        });
  });

  public static final Codec<SimpleDataMap> PRICE_MAP = SELL_DATA.listOf()
      .xmap(
          itemSellData -> {
            SimpleDataMap map = new SimpleDataMap();
            for (ItemSellData data : itemSellData) {
              map.add(data);
            }
            return map;
          },
          priceMap -> {
            List<ItemSellData> dataList = new ArrayList<>();
            Iterator<ItemSellData> it = priceMap.nonRepeatingIterator();

            while (it.hasNext()) {
              dataList.add(it.next());
            }

            return dataList;
          }
      );

  static final Codec<Map<String, PriceFileEntry>> MAP_CODEC
      = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, PriceFileEntry.CODEC);
}
