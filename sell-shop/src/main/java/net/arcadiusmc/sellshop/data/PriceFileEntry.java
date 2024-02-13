package net.arcadiusmc.sellshop.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.arcadiusmc.utils.io.ExtraCodecs;

record PriceFileEntry(List<String> files, boolean global) {

  static final Codec<PriceFileEntry> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.listOrValue(ExtraCodecs.KEY_CODEC).fieldOf("sources")
                .forGetter(p -> p.files),

            Codec.BOOL.optionalFieldOf("global", false)
                .forGetter(o -> o.global)
        )
        .apply(instance, PriceFileEntry::new);
  });

}
