package net.arcadiusmc.dungeons.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.dungeons.LevelBiome;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.nbt.CompoundTag;

@Getter
public class RoomType extends PieceType<RoomPiece> {

  private static final Codec<Map<LevelBiome, String>> BIOME_MAP_CODEC
      = Codec.unboundedMap(ExtraCodecs.enumCodec(LevelBiome.class), ExtraCodecs.KEY_CODEC);

  public static final Codec<RoomType> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.KEY_CODEC.fieldOf("structure")
                .forGetter(RoomType::getStructureName),

            ExtraCodecs.set(ExtraCodecs.enumCodec(RoomFlag.class))
                .optionalFieldOf("properties", Set.of())
                .forGetter(RoomType::getFlags),

            BIOME_MAP_CODEC.optionalFieldOf("palettes")
                .forGetter(roomType -> Optional.of(roomType.biome2Palette))
        )
        .apply(instance, (struct, flags, biomes) -> {
          EnumSet<RoomFlag> flagSet = EnumSet.noneOf(RoomFlag.class);
          flagSet.addAll(flags);

          return new RoomType(struct, flagSet, biomes.orElse(Collections.emptyMap()));
        });
  });

  private final EnumSet<RoomFlag> flags;
  private final Map<LevelBiome, String> biome2Palette;

  public RoomType(
      String structureName,
      EnumSet<RoomFlag> flags,
      Map<LevelBiome, String> biome2Palette
  ) {
    super(structureName);
    this.flags = flags;
    Objects.requireNonNull(biome2Palette);

    this.biome2Palette = biome2Palette.isEmpty()
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(biome2Palette);
  }

  public boolean hasFlag(RoomFlag flag) {
    return flags.contains(flag);
  }

  public String getPalette(LevelBiome biome) {
    return biome2Palette.getOrDefault(
        biome,
        BlockStructure.DEFAULT_PALETTE_NAME
    );
  }

  public EnumSet<RoomFlag> getFlags() {
    return flags.clone();
  }

  @Override
  public RoomPiece create() {
    return new RoomPiece(this);
  }

  @Override
  public RoomPiece load(CompoundTag tag) {
    return new RoomPiece(this, tag);
  }
}