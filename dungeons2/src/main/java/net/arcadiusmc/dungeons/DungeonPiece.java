package net.arcadiusmc.dungeons;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.Visitor.Result;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.math.Transform;
import net.forthecrown.nbt.CompoundTag;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public final class DungeonPiece {

  public static final int START_DEPTH = 1;

  private final UUID uniqueId;

  private Holder<BlockStructure> structure;
  private String paletteName = BlockStructure.DEFAULT_PALETTE_NAME;
  private PieceKind kind = PieceKind.MOB_ROOM;

  private Bounds3i boundingBox = Bounds3i.EMPTY;
  private Rotation rotation = Rotation.NONE;

  private Doorway[] doorways;
  private int depth = START_DEPTH;

  public DungeonPiece() {
    this.uniqueId = UUID.randomUUID();
  }

  public DungeonPiece(Holder<BlockStructure> structure) {
    this();

    Objects.requireNonNull(structure, "Null structure");

    this.structure = structure;
    initializeFromStructure();
  }

  private void initializeFromStructure() {
    BlockStructure value = structure.getValue();
    Vector3i size = value.getDefaultSize();

    boundingBox = Bounds3i.of(Vector3i.ZERO, size);
    rotation = Rotation.NONE;

    loadGates();
  }

  public static Stream<FunctionInfo> filterGates(Stream<FunctionInfo> stream) {
    return stream
        .filter(functionInfo -> {
          return functionInfo.getFacing().isRotatable()
              && functionInfo.getFunctionKey().equals(LevelFunctions.CONNECTOR);
        });
  }

  public static Stream<Doorway> mapToGates(Stream<FunctionInfo> stream) {
    return filterGates(stream).map(DungeonPiece::loadGate);
  }

  public static Doorway loadGate(FunctionInfo info) {
    Doorway doorway = new Doorway();

    Vector3i offset = info.getOffset();
    boolean applyCorrection = true;

    if (info.getTag() != null) {
      CompoundTag tag = info.getTag();

      Opening opening = Opening.load(tag.get("opening"));
      doorway.setOpening(opening);
      doorway.setStairs(tag.getBoolean("stairs", false));

      applyCorrection = tag.getBoolean("autocorrect_placement", true);
    }

    if (applyCorrection) {
      offset = offset.add(info.getFacing().getMod()).sub(0, 1, 0);
    }

    doorway.setCenter(offset);
    doorway.setDirection(info.getFacing());

    return doorway;
  }

  private void loadGates() {
    List<FunctionInfo> list
        = filterGates(structure.getValue().getFunctions().stream())
        .toList();

    doorways = new Doorway[list.size()];

    for (int i = 0; i < list.size(); i++) {
      FunctionInfo info = list.get(i);
      processGateInfo(info, i);
    }
  }

  private void processGateInfo(FunctionInfo info, int idx) {
    Doorway doorway = loadGate(info);
    doorway.setFrom(this);
    doorways[idx] = doorway;
  }

  /* --------------------------- Tree ---------------------------- */

  public void setDepth(int depth) {
    this.depth = depth;

    if (doorways == null) {
      return;
    }

    for (Doorway doorway : doorways) {
      if (doorway == null || doorway.isEntrance()) {
        continue;
      }

      doorway.onDepthPropagate(depth + 1);
    }
  }

  public DungeonPiece getRoot() {
    DungeonPiece parent = this;

    while (true) {
      DungeonPiece p = parent.getParent();

      if (p == null) {
        return parent;
      }

      parent = p;
    }
  }

  public DungeonPiece getParent() {
    Doorway parentConnector = getEntrance();
    if (parentConnector == null) {
      return null;
    }

    return parentConnector.getTo().getFrom();
  }

  public Doorway getEntrance() {
    for (Doorway doorway : doorways) {
      if (!doorway.isEntrance() || doorway.getTo() == null) {
        continue;
      }

      return doorway;
    }

    return null;
  }

  public Doorway getFirstFreeExit() {
    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }
      if (doorway.getTo() != null) {
        continue;
      }

      return doorway;
    }

    return null;
  }

  public boolean hasChildren() {
    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }

      if (doorway.getTo() == null) {
        continue;
      }

      return true;
    }

    return false;
  }

  public void clearChildren() {
    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }

      doorway.disconnect();
    }
  }

  public void forEachDescendant(Consumer<DungeonPiece> consumer) {
    consumer.accept(this);

    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }

      var to = doorway.getTo();
      if (to == null) {
        continue;
      }

      var child = to.getFrom();
      if (child.depth <= this.depth) {
        continue;
      }

      child.forEachDescendant(consumer);
    }
  }

  public Result visit(Visitor visitor) {
    Result r = visitor.visit(this);

    if (r == Result.BREAK) {
      return Result.BREAK;
    }
    if (r == Result.SKIP_CHILDREN) {
      return Result.CONTINUE;
    }

    for (Doorway doorway : doorways) {
      if (doorway.isEntrance()) {
        continue;
      }

      Doorway to = doorway.getTo();
      if (to == null) {
        continue;
      }

      var child = to.getFrom();
      if (child.depth <= this.depth) {
        continue;
      }

      r = child.visit(visitor);

      if (r == Result.BREAK) {
        return Result.BREAK;
      } else if (r == Result.SKIP_SIBLINGS) {
        break;
      }
    }

    return Result.CONTINUE;
  }

  public int testSelfReference() {
    int r = 0;

    for (Doorway doorway : doorways) {
      if (doorway == null || doorway.isEntrance()) {
        continue;
      }

      var to = doorway.getTo();
      if (to == null) {
        continue;
      }

      r += to.getFrom().testSelfReference();
    }

    return r;
  }

  /* --------------------------- Math and transformation ---------------------------- */

  public void transform(Transform transform) {
    if (transform.getRotation() != Rotation.NONE) {
      rotation = rotation.add(transform.getRotation());
    }

    boundingBox = Bounds3i.of(
        transform.apply(boundingBox.min()),
        transform.apply(boundingBox.max())
    );

    for (Doorway doorway : doorways) {
      if (doorway == null) {
        continue;
      }

      doorway.transform(transform);
    }
  }

  public Vector3i getPivotPoint() {
    return pivotPoint(boundingBox, rotation);
  }

  public static Vector3i pivotPoint(Bounds3i bb, Rotation rotation) {
    Vector3i min = bb.min();
    Vector3i max = bb.max();

    return switch (rotation) {
      case NONE                 -> min;
      case CLOCKWISE_90         -> min.withX(max.x());
      case COUNTERCLOCKWISE_90  -> min.withZ(max.z());
      case CLOCKWISE_180        -> max.withY(min.y());
    };
  }

  /* ------------------------------------------------------- */

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(",", getClass().getSimpleName() + "[", "]");
    if (structure != null) {
      joiner.add("structure=" + structure.getKey());
    }

    joiner.add("boundingBox=" + boundingBox);
    joiner.add("rotation=" + rotation);
    joiner.add("depth=" + depth);

    return joiner.toString();
  }
}
