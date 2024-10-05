package net.arcadiusmc.dungeons.gen;

import static net.arcadiusmc.dungeons.gen.StepResult.FAILED;
import static net.arcadiusmc.dungeons.gen.StepResult.MAX_DEPTH;
import static net.arcadiusmc.dungeons.gen.StepResult.MAX_SECTION_DEPTH;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.Doorway;
import net.arcadiusmc.dungeons.GenerationParameters;
import net.arcadiusmc.dungeons.Opening;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.utils.WeightedList;
import net.arcadiusmc.utils.WeightedList.WeightedIterator;
import org.apache.commons.lang3.Range;

@Getter
public class PieceGenerator {

  static final int DEFAULT_WEIGHT = 10;

  private final int depth;
  private final Doorway originGate;

  private final StructureGenerator gen;
  private final DungeonConfig config;
  private final GenerationParameters params;
  private final Random random;

  private final int sectionDepth;
  private final PieceGenerator parent;
  private final SectionType sectionType;
  private final SectionData data;

  private final WeightedList<PieceType> potentialRooms = new WeightedList<>();
  private final List<String> failedTypes = new ObjectArrayList<>();

  public PieceGenerator(
      SectionType type,
      PieceGenerator parent,
      StructureGenerator gen,
      Doorway originGate
  ) {
    this.originGate = originGate;
    this.gen = gen;
    this.config = gen.getConfig();
    this.params = config.getParameters();
    this.random = gen.getRandom();
    this.depth = originGate.getFrom().getDepth() + 1;
    this.sectionType = type;
    this.parent = parent;

    DungeonConfig cfg = gen.getConfig();
    GenerationParameters params = cfg.getParameters();

    if (parent == null || parent.sectionType != type) {
      Range<Integer> depthRange = type.getDepthRange(params);
      int optimal = depthRange.getMinimum();

      if (!Objects.equals(depthRange.getMinimum(), depthRange.getMaximum())) {
        optimal = gen.getRandom().nextInt(depthRange.getMinimum(), depthRange.getMaximum() + 1);
      }

      data = new SectionData(depthRange.getMaximum() * 2, optimal, depthRange);
      sectionDepth = DungeonPiece.START_DEPTH;
    } else {
      sectionDepth = parent.sectionDepth + 1;
      data = parent.data;
    }

    fillPotentials();
  }

  private void fillPotentials() {
    sectionType
        .fillPotentials(depth, gen.getConfig())
        .filter(pair -> {
          Opening opening = originGate.getOpening();
          List<Doorway> gates = pair.second().getGates();
          int matchingOpenings = 0;

          for (Doorway gate : gates) {
            if (gate.getOpening().equals(opening)) {
              matchingOpenings++;
            }
          }

          return !data.getSuccessful().contains(pair.second().getHolder().getKey())
              && matchingOpenings > 0;
        })
        .forEach(pair -> {
          potentialRooms.add(pair.firstInt(), pair.second());
        });
  }

  private StepResult sectionDepthFailure() {
    return StepResult.failure(MAX_SECTION_DEPTH);
  }

  public StepResult genStep() {
    if (potentialRooms.isEmpty()) {
      return StepResult.failure(FAILED);
    }

    if (sectionDepth > data.getDepthRange().getMaximum()) {
      return sectionDepthFailure();
    }
    if (sectionDepth > data.getOptimalDepth() && random.nextInt(4) == 0) {
      return sectionDepthFailure();
    }
    if ((data.getRoomCount() + 1) > data.getMaxRooms()) {
      return sectionDepthFailure();
    }

    if (depth > params.getDepthRange().getMaximum()) {
      return StepResult.failure(MAX_DEPTH);
    }

    // This is a weighted iterator, it iterates through the list's entries
    // in a semi-random order, which is dictated by the weight of each entry
    // and the return values of the random we're giving it
    WeightedIterator<PieceType> it = potentialRooms.iterator(random);

    // Make as many attempts as possible to find correct room
    while (it.hasNext()) {
      PieceType next = it.next();
      DungeonPiece piece = next.createPiece();

      // Create gates and shuffle list order
      Doorway[] gates = piece.getDoorways();
      ObjectArrays.shuffle(gates, random);

      // Remove this room from the potentials list regardless
      // if it succeeds the valid placement check or not, this
      // is so if a child node is backtracked to this node,
      // then we don't end up placing this node again
      it.remove();
      Doorway entrance = findValidEntrance(gates, piece);

      if (entrance == null) {
        continue;
      }

      return StepResult.success(entrance, next);
    }

    return StepResult.failure(FAILED);
  }

  private Doorway findValidEntrance(Doorway[] gates, DungeonPiece piece) {
    for (Doorway g : gates) {
      if (g.isStairs() && originGate.isStairs()) {
        continue;
      }

      originGate.align(g);

      if (!gen.isValidPlacement(piece)) {
        continue;
      }

      return g;
    }

    return null;
  }

  public void onChildFail(Holder<BlockStructure> structure) {
    failedTypes.add(structure.getKey());
    data.roomCount--;
    data.getSuccessful().remove(structure.getKey());
    gen.removeChildren(originGate.getFrom());
  }

  public void onSuccess(DungeonPiece piece) {
    data.roomCount++;
    data.getSuccessful().add(piece.getStructure().getKey());
  }

  public PieceGenerator sectionRoot() {
    PieceGenerator p = this;

    while (p.parent != null && p.parent.sectionType == this.sectionType) {
      p = p.parent;
    }

    return p;
  }
}
