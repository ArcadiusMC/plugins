package net.arcadiusmc.dungeons.gate;

import static net.arcadiusmc.dungeons.gate.GateData.TAG_OPENING;
import static net.arcadiusmc.dungeons.gate.GateData.TAG_STAIR;

import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Rotation;
import net.arcadiusmc.utils.io.TagUtil;
import net.arcadiusmc.utils.math.Transform;
import net.arcadiusmc.utils.math.Vectors;
import org.spongepowered.math.vector.Vector3i;

public record AbsoluteGateData(
    Direction direction,
    Vector3i center,
    boolean stairs,
    GateData.Opening opening
) {

  private static final String
      TAG_DIRECTION = "direction",
      TAG_CENTER = "center";

  public Vector3i rightSide() {
    var right = direction.right();
    var halfWidth = opening.width() / 2;

    return center.add(right.getMod().mul(halfWidth, 0, halfWidth))
        .sub(direction.getMod());
  }

  public AbsoluteGateData apply(Transform transform) {
    var dir = direction;

    if (transform.getRotation() != Rotation.NONE) {
      dir = direction.rotate(transform.getRotation());
    }

    return new AbsoluteGateData(dir, transform.apply(center), stairs, opening);
  }

  public CompoundTag save() {
    CompoundTag result = BinaryTags.compoundTag();
    result.put(TAG_DIRECTION, TagUtil.writeEnum(direction));
    result.put(TAG_CENTER, Vectors.writeTag(center));
    result.put(TAG_OPENING, opening.save());
    return result;
  }

  public static AbsoluteGateData load(BinaryTag t) {
    if (!(t instanceof CompoundTag tag)) {
      return null;
    }

    Direction dir = TagUtil.readEnum(Direction.class, tag.get(TAG_DIRECTION));
    Vector3i center = Vectors.read3i(tag.get(TAG_CENTER));
    GateData.Opening opening = GateData.Opening.load(tag.get(TAG_OPENING));
    boolean stairs = tag.getBoolean(TAG_STAIR);

    return new AbsoluteGateData(dir, center, stairs, opening);
  }
}