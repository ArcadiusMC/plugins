package net.arcadiusmc.dungeons.gen;

import static net.arcadiusmc.dungeons.gen.PieceGenerator.DEFAULT_WEIGHT;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import java.util.stream.Stream;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.GenerationParameters;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.dungeons.PieceKind;
import org.apache.commons.lang3.Range;

public enum SectionType {
  ROOM {
    @Override
    public Range<Integer> getDepthRange(GenerationParameters params) {
      return params.getRoomDepth();
    }

    @Override
    public int getMaxExits(GenerationParameters params) {
      return params.getMaxRoomExits();
    }

    @Override
    public Stream<IntObjectPair<PieceType>> fillPotentials(int depth, DungeonConfig config) {
      return config.getMatchingPieces(PieceKind.MOB_ROOM)
          .map(type -> {
            int weight = DEFAULT_WEIGHT;
            int exitCount = type.getGateCount() - 1;

            int maxExits = getMaxExits(config.getParameters());

            weight += exitCount < maxExits ? exitCount * 2 : -(exitCount * 2);

            return IntObjectPair.of(weight, type);
          });
    }
  },

  CONNECTOR {
    @Override
    public Range<Integer> getDepthRange(GenerationParameters params) {
      return params.getConnectorDepth();
    }

    @Override
    public int getMaxExits(GenerationParameters params) {
      return params.getMaxConnectorExits();
    }

    @Override
    public Stream<IntObjectPair<PieceType>> fillPotentials(int depth, DungeonConfig config) {
      return config.getMatchingPieces(PieceKind.CONNECTOR)
          .map(type -> {
            int weight = DEFAULT_WEIGHT;
            int exitCount = type.getGateCount() - 1;

            int minDepth = config.getParameters().getDepthRange().getMinimum();
            int maxExits = getMaxExits(config.getParameters());

            weight += depth < minDepth ? exitCount : -exitCount;
            weight += exitCount < maxExits ? exitCount * 2 : -(exitCount * 2);

            return IntObjectPair.of(weight, type);
          });
    }
  },
  ;

  public SectionType next() {
    SectionType[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  public abstract Range<Integer> getDepthRange(GenerationParameters params);

  public abstract int getMaxExits(GenerationParameters params);

  public abstract Stream<IntObjectPair<PieceType>> fillPotentials(int depth, DungeonConfig config);
}
