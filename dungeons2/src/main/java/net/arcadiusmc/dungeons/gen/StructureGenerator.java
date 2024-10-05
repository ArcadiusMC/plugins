package net.arcadiusmc.dungeons.gen;

import static net.arcadiusmc.dungeons.gen.StepResult.FAILED;
import static net.arcadiusmc.dungeons.gen.StepResult.MAX_DEPTH;
import static net.arcadiusmc.dungeons.gen.StepResult.MAX_SECTION_DEPTH;
import static net.arcadiusmc.dungeons.gen.StepResult.SUCCESS;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.Doorway;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.Opening;
import net.arcadiusmc.dungeons.PieceKind;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.dungeons.Visitor;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockPalette;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

@Getter
public class StructureGenerator {

  static final Comparator<DungeonPiece> DESCENDING_DEPTH
      = Comparator.comparingInt(DungeonPiece::getDepth)
      .reversed();

  private static final int MAX_FAILED_STEPS = 55;

  private static final Logger LOGGER = Loggers.getLogger();

  private final DungeonConfig config;
  private final Random random;

  private DungeonPiece rootPiece;

  private final Deque<PieceGenerator> genQueue = new ArrayDeque<>();
  private final Map<Doorway, PieceGenerator> doorToGen = new Object2ObjectOpenHashMap<>();
  private boolean finished = false;

  private int failedSteps = 0;
  private int steps = 0;

  private int lastGenResult = SUCCESS;
  private int sameResultsInARow = 0;

  PieceOverlapTestVisitor overlapTestVisitor;

  public StructureGenerator(DungeonConfig config, Random random) {
    this.config = config;
    this.random = random;
  }

  public void initialize() {
    List<PieceType> rootTypes = config.getMatchingPiecesList(PieceKind.ROOT);

    if (rootTypes.isEmpty()) {
      LOGGER.error("No root room found");
      return;
    }

    PieceType rootType;

    if (rootTypes.size() == 1) {
      rootType = rootTypes.getFirst();
    } else {
      rootType = rootTypes.get(random.nextInt(rootTypes.size()));
    }

    DungeonPiece piece = rootType.createPiece();
    rootPiece = piece;

    List<Doorway> exits = generateExitGates(piece, Integer.MAX_VALUE);
    enqueueExits(exits, SectionType.CONNECTOR, null);
  }

  private void enqueueExits(List<Doorway> exits, SectionType type, PieceGenerator parent) {
    for (Doorway exit : exits) {
      PieceGenerator generator = new PieceGenerator(type, parent, this, exit);
      pushLast(generator);
    }
  }

  public void generate() {
    initialize();

    while (!finished) {
      genStep();
    }

    postGeneration();
  }

  public void postGeneration() {
    attachBossRoom();
    removeDeadEnds();
    closeEndingGates();
    decorateGates();
  }

  public int decorateGates() {
    var op = new DecorateGates();
    rootPiece.forEachDescendant(op);
    return op.added;
  }

  public String closeEndingGates() {
    CloseOpenExits closeOpen = new CloseOpenExits();
    rootPiece.forEachDescendant(closeOpen);

    return String.format("closed-gates=%s, added-gates=%s",
        closeOpen.gatesClosed, closeOpen.gatesAdded
    );
  }

  public String attachBossRoom() {
    List<PieceType> bossRoomList = config.getMatchingPiecesList(PieceKind.BOSS_ROOM);
    if (bossRoomList.isEmpty()) {
      LOGGER.warn("No boss rooms in config, can't add boss room");
      return "No boss room in config";
    }

    FindDeepestGate visitor = new FindDeepestGate();
    rootPiece.forEachDescendant(visitor);

    List<DungeonPiece> endGates = visitor.endGates;
    if (endGates.isEmpty()) {
      return "endGates empty";
    }

    endGates.sort(DESCENDING_DEPTH);
    Collections.shuffle(bossRoomList, random);

    for (DungeonPiece endGate : endGates) {
      Doorway exit = openGate(endGate);
      if (exit == null) {
        continue;
      }

      for (PieceType pieceType : bossRoomList) {
        DungeonPiece bossRoom = pieceType.createPiece();
        Doorway[] doors = bossRoom.getDoorways();

        for (Doorway door : doors) {
          exit.align(door);

          if (!isValidPlacement(bossRoom)) {
            continue;
          }

          exit.connect(door);
          return null;
        }
      }
    }

    return "No valid placement for any boss room found";
  }

  public int removeDeadEnds() {
    // List of connector pieces that have no rooms connected to them
    Deque<DungeonPiece> deadEnds = new LinkedList<>();

    rootPiece.forEachDescendant(piece -> {
      if (piece.hasChildren()) {
        return;
      }
      if (!isGate(piece)) {
        return;
      }

      DungeonPiece parent = piece.getParent();
      if (parent == null || parent.getKind() != PieceKind.CONNECTOR) {
        return;
      }

      deadEnds.addLast(parent);
    });

    int removed = 0;

    while (!deadEnds.isEmpty()) {
      DungeonPiece first = deadEnds.pollFirst();

      if (first.getParent() == null) {
        continue;
      }

      if (!isDeadEnd(first)) {
        continue;
      }

      Doorway entrance = first.getEntrance();
      if (entrance == null) {
        continue;
      }

      DungeonPiece to = entrance.getTo().getFrom();
      entrance.getTo().disconnect();
      deadEnds.addLast(to);

      removed++;
    }

    return removed;
  }

  private boolean isDeadEnd(DungeonPiece piece) {
    //
    // A piece is a 'dead end' if all the following conditions are met:
    // - The piece is NOT a room (mob_room, boss_room)
    // - The piece has no non-gate children OR
    // - All children are themselves dead ends
    //

    if (piece.hasChildren()) {
      Doorway[] doorways = piece.getDoorways();

      for (Doorway doorway : doorways) {
        if (doorway.isEntrance()) {
          continue;
        }

        Doorway to = doorway.getTo();
        if (to == null) {
          continue;
        }

        DungeonPiece p = to.getFrom();
        if (!isDeadEnd(p)) {
          return false;
        }
      }

      return true;
    }

    return switch (piece.getKind()) {
      case ROOT, MOB_ROOM, BOSS_ROOM -> false;

      case CLOSED_GATE, DECORATED_GATE, OPEN_GATE -> {
        DungeonPiece parent = piece.getParent();
        if (parent == null) {
          yield true;
        }

        PieceKind k = parent.getKind();
        switch (k) {
          case ROOT:
          case MOB_ROOM:
          case BOSS_ROOM:
            yield false;

          default:
            yield true;
        }
      }

      default -> true;
    };
  }

  private boolean isGate(DungeonPiece piece) {
    return switch (piece.getKind()) {
      case DECORATED_GATE, OPEN_GATE, CLOSED_GATE -> true;
      default -> false;
    };
  }

  private void pushLast(PieceGenerator generator) {
    doorToGen.put(generator.getOriginGate(), generator);
    genQueue.addLast(generator);
  }

  private void pushFirst(PieceGenerator generator) {
    doorToGen.put(generator.getOriginGate(), generator);
    genQueue.addFirst(generator);
  }

  public PieceGenerator poll() {
    PieceGenerator gen = genQueue.poll();

    if (gen == null) {
      return null;
    }

    doorToGen.remove(gen.getOriginGate());
    return gen;
  }

  public void removeChildren(DungeonPiece piece) {
    Doorway[] doorways = piece.getDoorways();
    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }

      PieceGenerator removed = doorToGen.remove(doorway);
      if (removed != null) {
        genQueue.remove(removed);
      }

      Doorway doorwayTo = doorway.getTo();
      if (doorwayTo == null) {
        continue;
      }

      removeChildren(doorwayTo.getFrom());
    }

    piece.clearChildren();
  }

  public StepResult genStep() {
    PieceGenerator piece = poll();

    if (piece == null) {
      finished = true;
      return null;
    }

    steps++;

    Doorway originGate = piece.getOriginGate();
    StepResult result = piece.genStep();

    if (result.code() == lastGenResult) {
      sameResultsInARow++;
    } else {
      lastGenResult = result.code();
      sameResultsInARow = 0;
    }

    switch (result.code()) {
      //
      // When piece generation succeeds, the following happens:
      //
      //  1. The generated piece is attached to the doorway it was generated for.
      //
      //  2. Each exit of the created room has a gate piece placed on it. The
      //     amount of gates that are 'open' and 'closed' depends on the piece's
      //     kind and the config parameters.
      //
      //  3. The exits to the generated 'open' gates are added to the generation
      //     queue.
      //
      case SUCCESS -> {
        DungeonPiece generatedPiece = result.entrance().getFrom();

        piece.onSuccess(generatedPiece);
        originGate.connect(result.entrance());

        int maxExits = piece.getSectionType().getMaxExits(config.getParameters());
        List<Doorway> gateExits = generateExitGates(generatedPiece, maxExits);

        enqueueExits(gateExits, piece.getSectionType(), piece);
      }

      //
      // When a node fails to generate one of two steps should be taken
      //
      //  1. The door to the node is closed and this branch is considered 'complete'
      //
      //  2. We back track one node upward and we remove the generated node,
      //     so we can have that parent reattempt generation to hopefully
      //     provide a better result
      //
      // The outcome chosen is dependent on how many times the generator has
      // failed to generate the piece. If it's failed too many times, it should
      // give up.
      //
      case FAILED -> {
        PieceGenerator parent = piece.getParent();
        failedSteps++;

        if (parent == null || failedSteps > MAX_FAILED_STEPS) {
          closeGate(originGate);
          failedSteps = 0;

          return result;
        }

        // Get the room the gate piece is attached to
        DungeonPiece gateParent = originGate.getFrom().getParent();

        if (gateParent == null) {
          LOGGER.warn("gateparent == null???");
        } else {
          parent.onChildFail(gateParent.getStructure());
          pushFirst(parent);
        }
      }

      //
      // Max depth reached means the tree isn't allowed to grow any deeper
      //
      // If the current section's depth is less than the 'optimal' depth for
      // the section, then we rewind back to the root of the current section
      // and reattempt generation from there.
      //
      // Rewinding to root also applies when attempting to generate a
      // connector child??????
      //
      // What???
      //
      case MAX_DEPTH -> {
        PieceGenerator parent = piece.getParent();

        //
        // FIXME: this is a bandaid fix to the generator
        //    entering a never ending loop where it constantly
        //    returns ERR_MAX_DEPTH and doesn't stop.
        //
        if (sameResultsInARow > 50) {
          return result;
        }

        if (piece.getSectionDepth() < piece.getData().getOptimalDepth()
            || (parent != null && parent.getSectionType() == SectionType.CONNECTOR)
        ) {
          PieceGenerator root = piece.sectionRoot();

          if (root == null || root == piece) {
            closeGate(originGate);
            return result;
          }

          removeChildren(root.getOriginGate().getFrom());
          pushFirst(root);

        } else if (parent != null && parent.getSectionType() == SectionType.ROOM) {
          closeGate(originGate);
        }
      }

      //
      // Max section depth, just swap the section type to the next one
      // connector -> room, room -> connector
      //
      case MAX_SECTION_DEPTH -> {
        PieceGenerator generator = switchGeneratorType(piece, originGate, piece.getParent());
        pushFirst(generator);
      }

      default -> throw new IllegalStateException("Unexpected value: " + result.code());
    }

    return result;
  }

  PieceGenerator switchGeneratorType(
      PieceGenerator existing,
      Doorway origin,
      PieceGenerator parent
  ) {
    SectionType type = existing.getSectionType().next();
    return new PieceGenerator(type, parent, this, origin);
  }

  private void closeGate(Doorway gateExit) {
    DungeonPiece gate = gateExit.getFrom();
    closeGate(gate);
  }

  private void closeGate(DungeonPiece gate) {
    Doorway gateEntrance = gate.getEntrance();
    if (gateEntrance == null) {
      return;
    }

    Doorway parentExit = gateEntrance.getTo();
    if (parentExit == null) {
      return;
    }

    Doorway matchingClosed = createMatchingSizeGateEntrance(parentExit.getOpening(), false);
    if (matchingClosed == null) {
      return;
    }

    parentExit.disconnect();
    parentExit.connect(matchingClosed);
  }

  private Doorway openGate(DungeonPiece gate) {
    if (gate.getKind() == PieceKind.OPEN_GATE) {
      return gate.getFirstFreeExit();
    }

    Doorway entrance = gate.getEntrance();
    if (entrance == null) {
      return null;
    }

    Doorway parentExit = entrance.getTo();
    if (parentExit == null) {
      return null;
    }

    Doorway matchingOpen = createMatchingSizeGateEntrance(parentExit.getOpening(), true);
    if (matchingOpen == null) {
      return null;
    }

    parentExit.disconnect();
    parentExit.connect(matchingOpen);

    return matchingOpen.getFrom().getFirstFreeExit();
  }

  public List<Doorway> generateExitGates(DungeonPiece piece, int maxExits) {
    Doorway[] doorways = piece.getDoorways();
    List<Doorway> result = new ObjectArrayList<>();

    int exits = doorways.length - 1;
    int closedAmount = Math.max(0, exits - maxExits);

    for (Doorway pieceExit : doorways) {
      if (pieceExit.getTo() != null || pieceExit.isEntrance()) {
        continue;
      }

      boolean open = closedAmount < 1;
      Doorway matchingEntrance = createMatchingSizeGateEntrance(pieceExit.getOpening(), open);

      if (!open) {
        closedAmount--;
      }

      if (matchingEntrance == null) {
        continue;
      }

      pieceExit.connect(matchingEntrance);
      Doorway[] gateExits = matchingEntrance.getFrom().getDoorways();

      for (Doorway gateExit : gateExits) {
        if (matchingEntrance.isStairs()) {
          gateExit.setStairs(true);
        }

        if (gateExit.isEntrance()
            || gateExit.getTo() != null
            || gateExit == matchingEntrance
        ) {
          continue;
        }

        result.add(gateExit);
      }
    }

    return result;
  }

  private static boolean testGateType(PieceType type, boolean open) {
    if (open) {
      return type.getKind() == PieceKind.OPEN_GATE;
    } else {
      return type.getKind() == PieceKind.CLOSED_GATE;
    }
  }

  public Doorway createMatchingSizeGateEntrance(Opening opening, boolean open) {
    List<PieceType> matchingGates = config.getPieceTypes()
        .stream()
        .filter(pieceType -> testGateType(pieceType, open))
        .filter(pieceType -> {
          for (Doorway doorway : pieceType.getGates()) {
            if (!doorway.getOpening().equals(opening)) {
              continue;
            }

            return true;
          }

          return false;
        })
        .toList();

    if (matchingGates.isEmpty()) {
      matchingGates = config.getPieceTypes().stream()
          .filter(t -> t.getKind() == PieceKind.OPEN_GATE || t.getKind() == PieceKind.CLOSED_GATE)
          .toList();

      if (matchingGates.isEmpty()) {
        return null;
      }
    }

    PieceType gateType;

    if (matchingGates.size() == 1) {
      gateType = matchingGates.getFirst();
    } else {
      gateType = matchingGates.get(random.nextInt(matchingGates.size()));
    }

    DungeonPiece gate = gateType.createPiece();
    Doorway[] doorways = gate.getDoorways();

    for (Doorway doorway : doorways) {
      if (!doorway.getOpening().equals(opening)) {
        continue;
      }

      return doorway;
    }

    return null;
  }


  /* --------------------------- Placement testing ---------------------------- */

  public boolean isValidPlacement(DungeonPiece piece) {
    if (rootPiece == null) {
      return true;
    }

    Bounds3i bb = piece.getBoundingBox();
    BlockPalette palette = getPalette(piece);

    if (palette == null) {
      return true;
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    //LOGGER.debug("Starting bounds check");

    try {
      if (overlapTestVisitor == null) {
        overlapTestVisitor = new PieceOverlapTestVisitor();
      }

      overlapTestVisitor.overlaps = false;
      overlapTestVisitor.bb = bb;
      overlapTestVisitor.piece = piece;

      rootPiece.visit(overlapTestVisitor);

      return !overlapTestVisitor.overlaps;
    } finally {
      stopwatch.stop();
      Duration elapsed = stopwatch.elapsed();

      long millis = elapsed.toMillis();
      float seconds = ((float) millis) / 1000f;

      //LOGGER.debug("Bounds check took {}ms or {}sec", millis, seconds);
    }
  }

  private BlockPalette getPalette(DungeonPiece piece) {
    Holder<BlockStructure> holder = piece.getStructure();
    if (holder == null) {
      return null;
    }

    return holder.getValue().getPalette(piece.getPaletteName());
  }

  class PieceOverlapTestVisitor implements Visitor {

    DungeonPiece piece;
    Bounds3i bb;

    boolean overlaps = false;

    @Override
    public Result visit(DungeonPiece predicate) {
      Bounds3i valBb = predicate.getBoundingBox();

      if (Objects.equals(piece, predicate)) {
        return Result.CONTINUE;
      }

      if (!valBb.overlaps(bb)) {
        return Result.CONTINUE;
      }

      if (!overlaps(piece, predicate)) {
        return Result.CONTINUE;
      }

      overlaps = true;
      return Result.BREAK;
    }

    private boolean overlaps(DungeonPiece piece, DungeonPiece inWorld) {
      BlockPalette palette = getPalette(piece);
      BlockPalette inPalette = getPalette(inWorld);

      if (inPalette == null || palette == null) {
        return false;
      }

      Transform transform = Transform.offset(piece.getPivotPoint())
          .withRotation(piece.getRotation());

      Transform worldTransform = Transform.offset(inWorld.getPivotPoint())
          .withRotation(inWorld.getRotation());

      int size = 0;
      for (LongList value : inPalette.getBlock2Positions().values()) {
        size += value.size();
      }

      Set<Vector3i> illegalPositions = new ObjectOpenHashSet<>(size);
      inPalette.getBlock2Positions().forEach((blockInfo, longs) -> {
        if (blockInfo.getData().getMaterial().isAir()) {
          return;
        }

        for (int i = 0; i < longs.size(); i++) {
          long l = longs.getLong(i);
          Vector3i pos = worldTransform.apply(Vectors.fromLong(l));
          illegalPositions.add(pos);
        }
      });

      for (LongList value : palette.getBlock2Positions().values()) {
        for (int i = 0; i < value.size(); i++) {
          long l = value.getLong(i);
          Vector3i pos = transform.apply(Vectors.fromLong(l));

          if (illegalPositions.contains(pos)) {
            return true;
          }
        }
      }

      return false;
    }
  }

  /* --------------------------- Post processors ---------------------------- */

  class CloseOpenExits implements Consumer<DungeonPiece> {

    int gatesClosed = 0;
    int gatesAdded = 0;

    @Override
    public void accept(DungeonPiece piece) {
      if (isGate(piece)) {
        if (piece.hasChildren()) {
          return;
        }
        if (piece.getKind() != PieceKind.OPEN_GATE) {
          return;
        }

        closeGate(piece);
        gatesClosed++;

        return;
      }

      Doorway[] doorways = piece.getDoorways();
      for (Doorway doorway : doorways) {
        if (doorway.isEntrance()) {
          continue;
        }

        Doorway to = doorway.getTo();
        if (to != null) {
          continue;
        }

        Doorway matching = createMatchingSizeGateEntrance(doorway.getOpening(), false);
        if (matching == null) {
          return;
        }

        doorway.connect(matching);
        gatesAdded++;
      }
    }
  }

  class DecorateGates implements Consumer<DungeonPiece> {

    int added = 0;

    @Override
    public void accept(DungeonPiece piece) {
      if (piece.hasChildren()) {
        return;
      }
      if (piece.getKind() != PieceKind.CLOSED_GATE) {
        return;
      }

      float r = random.nextFloat();
      if (r > config.getParameters().getDecoratedGateChance()) {
        return;
      }

      List<PieceType> decoratedGates = config.getMatchingPiecesList(PieceKind.DECORATED_GATE);
      if (decoratedGates.isEmpty()) {
        return;
      }

      Collections.shuffle(decoratedGates, random);
      Doorway gateEntrance = piece.getEntrance();
      if (gateEntrance == null) {
        return;
      }

      Doorway parentExit = gateEntrance.getTo();
      if (parentExit == null) {
        return;
      }

      Doorway currentEnt = parentExit.getTo();
      parentExit.disconnect();

      for (PieceType decoratedGate : decoratedGates) {
        DungeonPiece gate = decoratedGate.createPiece();
        Doorway[] doorways = gate.getDoorways();

        for (Doorway doorway : doorways) {
          if (!doorway.getOpening().equals(parentExit.getOpening())) {
            continue;
          }

          parentExit.align(doorway);
          if (!isValidPlacement(gate)) {
            continue;
          }

          parentExit.connect(doorway);

          added++;
          return;
        }
      }

      parentExit.connect(currentEnt);
    }
  }

  class FindDeepestGate implements Consumer<DungeonPiece> {

    final List<DungeonPiece> endGates = new ObjectArrayList<>();

    @Override
    public void accept(DungeonPiece piece) {
      if (!isGate(piece)) {
        return;
      }

      if (piece.hasChildren()) {
        return;
      }

      endGates.add(piece);
    }
  }
}
