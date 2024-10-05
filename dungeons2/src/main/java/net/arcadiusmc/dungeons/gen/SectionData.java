package net.arcadiusmc.dungeons.gen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.Range;

@Getter
public class SectionData {
  private final int maxRooms;
  private final int optimalDepth;
  private final Range<Integer> depthRange;

  public int roomCount;
  private final List<String> successful = new ObjectArrayList<>();

  public SectionData(int maxRooms, int optimalDepth, Range<Integer> depthRange) {
    this.maxRooms = maxRooms;
    this.optimalDepth = optimalDepth;
    this.depthRange = depthRange;
  }
}
