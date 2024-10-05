package net.arcadiusmc.dungeons.gen;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.math.Direction;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class GeneratorFunction {

  private final String functionKey;
  private final CompoundTag data;
  private Vector3i position = Vector3i.ZERO;
  private Direction facing = Direction.WEST;

  public GeneratorFunction(String functionKey) {
    this.functionKey = functionKey;
    this.data = BinaryTags.compoundTag();
  }

}
