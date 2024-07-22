package net.arcadiusmc.dungeons;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.utils.math.Vectors;
import org.bukkit.entity.Player;

public class SessionMap {

  private final Map<UUID, DungeonSession> byPlayerId = new HashMap<>();
  private final Long2ObjectMap<DungeonSession> byCellId = new Long2ObjectOpenHashMap<>();
  private final LongSet usedCells = new LongOpenHashSet();

  public SessionMap() {

  }

  public void freeCell(long cellId) {
    byCellId.remove(cellId);
    usedCells.remove(cellId);
  }

  public long acquireCell() {
    long cellId = findFreeCell();
    usedCells.add(cellId);
    return cellId;
  }

  private long findFreeCell() {
    int cellX = 0;
    int cellZ = 0;
    int moveX = 1;
    int moveZ = 0;
    int segmentLength = 1;
    int segmentPassed = 0;
    int directionChanges = 0;

    long cellId = 0;

    while (true) {
      //Check cell state after movement, so 0,0 always remains free

      cellX += moveX;
      cellZ += moveZ;
      segmentPassed++;

      cellId = Vectors.toChunkLong(cellX, cellZ);
      if (!usedCells.contains(cellId)) {
        return cellId;
      }

      if (segmentPassed >= segmentLength) {
        segmentPassed = 0;
        directionChanges++;

        int temp = moveX;
        moveX = -moveZ;
        moveZ = temp;

        if (directionChanges % 2 == 0) {
          segmentLength++;
        }
      }
    }
  }

  public Optional<DungeonSession> getSession(Player player) {
    return Optional.ofNullable(byPlayerId.get(player.getUniqueId()));
  }

  void onRemovePlayer(UUID playerId) {
    byPlayerId.remove(playerId);
  }

  void onAddPlayer(UUID playerId, DungeonSession session) {
    byPlayerId.put(playerId, session);
  }
}
