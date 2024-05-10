package net.arcadiusmc.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.List;

public record ItemsConfig(
    boolean allowOpEnchants,
    boolean allowWearable,
    int[] wearableIds
) {

  static final ItemsConfig DEFAULT = new ItemsConfig(true, true, IntArrays.EMPTY_ARRAY);

  static final Codec<ItemsConfig> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.BOOL.lenientOptionalFieldOf("allow-overpowered-enchantments", true)
                .forGetter(ItemsConfig::allowOpEnchants),

            Codec.BOOL.lenientOptionalFieldOf("allow-wearable-tag", true)
                .forGetter(ItemsConfig::allowWearable),

            Codec.INT.listOf()
                .xmap(
                    integers -> {
                      int[] arr = new int[integers.size()];
                      for (int i = 0; i < integers.size(); i++) {
                        arr[i] = integers.get(i);
                      }
                      return arr;
                    },
                    ints -> {
                      List<Integer> list = new ArrayList<>();
                      for (int anInt : ints) {
                        list.add(anInt);
                      }
                      return list;
                    }
                )
                .lenientOptionalFieldOf("wearable-texture-ids", IntArrays.EMPTY_ARRAY)
                .forGetter(ItemsConfig::wearableIds)
        )
        .apply(instance, ItemsConfig::new);
  });
}
