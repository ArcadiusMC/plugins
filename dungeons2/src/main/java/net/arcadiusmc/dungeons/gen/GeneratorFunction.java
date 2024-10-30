package net.arcadiusmc.dungeons.gen;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.utils.math.Direction;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class GeneratorFunction {

  private final String functionKey;
  private final CompoundTag data;
  private final DungeonPiece containingPiece;
  private Vector3i position = Vector3i.ZERO;
  private Direction facing = Direction.WEST;
  private int depth = 0;

  public GeneratorFunction(String functionKey, DungeonPiece containingPiece) {
    this.functionKey = functionKey;
    this.containingPiece = containingPiece;

    this.data = BinaryTags.compoundTag();
  }

}
