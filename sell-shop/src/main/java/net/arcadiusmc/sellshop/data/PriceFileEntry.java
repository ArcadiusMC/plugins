package net.arcadiusmc.sellshop.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.utils.io.ExtraCodecs;

record PriceFileEntry(String fileName, boolean global) {

  static final Codec<PriceFileEntry> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.KEY_CODEC.fieldOf("file").forGetter(o -> o.fileName),
            Codec.BOOL.optionalFieldOf("global", false).forGetter(o -> o.global)
        )
        .apply(instance, PriceFileEntry::new);
  });

}
