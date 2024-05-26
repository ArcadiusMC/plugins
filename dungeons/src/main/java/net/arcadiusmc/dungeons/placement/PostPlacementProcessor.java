package net.arcadiusmc.dungeons.placement;

import java.util.List;
import java.util.Random;
import net.arcadiusmc.structure.FunctionInfo;

public interface PostPlacementProcessor {
  void processAll(
      LevelPlacement placement,
      List<FunctionInfo> markerList,
      Random random
  );
}