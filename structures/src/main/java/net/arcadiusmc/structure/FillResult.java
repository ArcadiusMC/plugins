package net.arcadiusmc.structure;

import lombok.Getter;
import lombok.Setter;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class FillResult {

  int blockCount = 0;
  int entityCount = 0;
  int functionCount = 0;
  Vector3i size = Vector3i.ZERO;

}
