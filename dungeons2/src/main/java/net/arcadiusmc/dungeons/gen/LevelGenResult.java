package net.arcadiusmc.dungeons.gen;

import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.DungeonPiece;

@Getter @Setter
public class LevelGenResult {

  static final Comparator<LevelGenResult> COMPARATOR
      = Comparator.comparingInt(LevelGenResult::getNonConnectorRooms)
      .reversed();

  private final DungeonPiece rootPiece;
  private int nonConnectorRooms = 0;
  private int greatestDepth = Integer.MIN_VALUE;

  public LevelGenResult(DungeonPiece rootPiece) {
    this.rootPiece = rootPiece;
  }
}
