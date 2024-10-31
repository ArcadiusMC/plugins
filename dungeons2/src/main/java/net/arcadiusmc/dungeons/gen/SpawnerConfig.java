package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import net.arcadiusmc.utils.io.ExistingObjectCodec;

public class SpawnerConfig {

  static final Codec<SpawnerConfig> CODEC = ExistingObjectCodec.createCodec(
      SpawnerConfig::new,
      builder -> {

      }
  );


}
