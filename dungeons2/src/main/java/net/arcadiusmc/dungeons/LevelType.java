package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.gen.DecorationPass;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import org.apache.commons.lang3.Range;

@Getter @Setter
public class LevelType {

  public static final Codec<LevelType> CODEC = ExistingObjectCodec.createCodec(
      LevelType::new,
      builder -> {
        builder.optional("deco-parameters", DecorationParameters.CODEC)
            .getter(LevelType::getDecoration)
            .setter(LevelType::setDecoration);

        builder.optional("pieces", PieceType.CODEC.listOf())
            .getter(LevelType::getPieceTypes)
            .setter((cfg, types) -> {
              cfg.pieceTypes.clear();
              cfg.pieceTypes.addAll(types);
            });

        builder.optional("decoration-passes", DecorationPass.CODEC.listOf())
            .getter(LevelType::getDecorationPasses)
            .setter((config, passes) -> {
              config.decorationPasses.clear();
              config.decorationPasses.addAll(passes);
            });

        builder.optional("display-name", Codec.STRING)
            .getter(LevelType::getName)
            .setter(LevelType::setName);

        builder.optional("level-depth-range", GenerationParameters.INT_RANGE_CODEC)
            .getter(LevelType::getLevelDepthRange)
            .setter(LevelType::setLevelDepthRange);
      }
  );

  private String name = null;
  private Range<Integer> levelDepthRange = Range.between(Integer.MIN_VALUE, Integer.MAX_VALUE);

  private DecorationParameters decoration = new DecorationParameters();
  private final List<PieceType> pieceTypes = new ObjectArrayList<>();
  private final List<DecorationPass> decorationPasses = new ObjectArrayList<>();

  public void applyTo(DungeonConfig config) {
    config.setDecoration(decoration);

    config.getDecorationPasses().clear();
    config.getPieceTypes().clear();

    config.getDecorationPasses().addAll(decorationPasses);
    config.getPieceTypes().addAll(pieceTypes);
  }
}
