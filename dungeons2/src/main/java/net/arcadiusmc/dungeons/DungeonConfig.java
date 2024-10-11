package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.gen.DecorationPass;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.math.Vectors;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class DungeonConfig {

  public static final Codec<DungeonConfig> CODEC
      = ExistingObjectCodec.<DungeonConfig>create(builder -> {
        builder.optional("location", Vectors.V3I_CODEC)
            .getter(DungeonConfig::getLocation)
            .setter(DungeonConfig::setLocation);

        builder.optional("potential-levels", Codec.INT)
            .getter(DungeonConfig::getPotentialLevels)
            .setter(DungeonConfig::setPotentialLevels);

        builder.optional("gen-parameters", GenerationParameters.CODEC)
            .getter(DungeonConfig::getParameters)
            .setter(DungeonConfig::setParameters);

        builder.optional("deco-parameters", DecorationParameters.CODEC)
            .getter(DungeonConfig::getDecoration)
            .setter(DungeonConfig::setDecoration);

        builder.optional("pieces", PieceType.CODEC.listOf())
            .getter(DungeonConfig::getPieceTypes)
            .setter((cfg, types) -> {
              cfg.pieceTypes.clear();
              cfg.pieceTypes.addAll(types);
            });

        builder.optional("decoration-passes", DecorationPass.CODEC.listOf())
            .getter(DungeonConfig::getDecorationPasses)
            .setter((config, passes) -> {
              config.decorationPasses.clear();
              config.decorationPasses.addAll(passes);
            });
      })
      .codec(Codec.unit(DungeonConfig::new));

  private GenerationParameters parameters = new GenerationParameters();
  private DecorationParameters decoration = new DecorationParameters();

  private final List<DecorationPass> decorationPasses = new ObjectArrayList<>();

  private Vector3i location = Vector3i.ZERO;
  private int potentialLevels = 35;

  private final List<PieceType> pieceTypes = new ObjectArrayList<>();

  public DungeonConfig() {

  }

  public Random createRandom() {
    return new Random(parameters.getSeed());
  }

  public Stream<PieceType> getMatchingPieces(PieceKind kind) {
    return pieceTypes.stream().filter(type -> type.getKind() == kind);
  }

  public List<PieceType> getMatchingPiecesList(PieceKind kind) {
    return getMatchingPieces(kind).collect(ObjectArrayList.toList());
  }
}
