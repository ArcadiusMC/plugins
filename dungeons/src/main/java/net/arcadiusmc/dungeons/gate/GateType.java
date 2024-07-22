package net.arcadiusmc.dungeons.gate;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.nbt.CompoundTag;

@Getter
public class GateType extends PieceType<GatePiece> {

  public static final Codec<GateType> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.KEY_CODEC.fieldOf("structure")
                .forGetter(GateType::getStructureName),

            ExtraCodecs.KEY_CODEC
                .optionalFieldOf("closed-palette", BlockStructure.DEFAULT_PALETTE_NAME)
                .forGetter(GateType::getClosedPalette),

            ExtraCodecs.KEY_CODEC
                .optionalFieldOf("open-palette")
                .forGetter(o -> Optional.ofNullable(o.openPalette))
        )
        .apply(instance, (struct, closed, open) -> {
          return new GateType(struct, open.orElse(null), closed);
        });
  });

  private final String openPalette;
  private final String closedPalette;

  public GateType(
      String structureName,
      String openPalette,
      String closedPalette
  ) {
    super(structureName);
    this.openPalette = openPalette;
    this.closedPalette = closedPalette;
  }

  public boolean isOpenable() {
    return !Strings.isNullOrEmpty(openPalette);
  }

  @Override
  public GatePiece create() {
    return new GatePiece(this);
  }

  @Override
  public GatePiece load(CompoundTag tag) {
    return new GatePiece(this, tag);
  }
}