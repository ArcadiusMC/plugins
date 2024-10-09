package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructuresPlugin;
import net.arcadiusmc.utils.io.ExtraCodecs;

@Getter
public class PieceType {

  static final Codec<PieceType> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            StructuresPlugin.getManager().getRegistry().holderCodec()
                .fieldOf("structure")
                .forGetter(PieceType::getHolder),

            Codec.STRING.optionalFieldOf("palette", BlockStructure.DEFAULT_PALETTE_NAME)
                .forGetter(PieceType::getPaletteName),

            ExtraCodecs.enumCodec(PieceKind.class)
                .fieldOf("type")
                .forGetter(PieceType::getKind)
        )
        .apply(instance, PieceType::new);
  });

  private final Holder<BlockStructure> holder;
  private final String paletteName;
  private final PieceKind kind;

  public PieceType(Holder<BlockStructure> holder, String paletteName, PieceKind kind) {
    this.holder = holder;
    this.paletteName = paletteName;
    this.kind = kind;
  }

  public DungeonPiece createPiece() {
    try {
      DungeonPiece piece = new DungeonPiece(holder);
      piece.setPaletteName(paletteName);
      piece.setKind(kind);
      return piece;
    } catch (RuntimeException exc) {
      throw new RuntimeException("Failed to create piece of type " + holder.getKey(), exc);
    }
  }

  public int getGateCount() {
    return (int) DungeonPiece.filterGates(holder.getValue().getFunctions().stream()).count();
  }

  public List<Doorway> getGates() {
    return DungeonPiece.mapToGates(holder.getValue().getFunctions().stream())
        .collect(Collectors.toList());
  }

}
