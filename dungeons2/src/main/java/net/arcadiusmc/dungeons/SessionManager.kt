package net.arcadiusmc.dungeons

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.math.Vectors
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.spongepowered.math.vector.Vector2i
import java.util.*


const val SECTION_CELL_BITSHIFT = 11
const val SECTION_CELL_SIZE = 1 shl SECTION_CELL_BITSHIFT
const val SECTION_CELL_AREA = SECTION_CELL_SIZE * SECTION_CELL_SIZE

fun toCell(cord: Int): Int {
  return cord shr SECTION_CELL_BITSHIFT
}

fun toWorld(cord: Int): Int {
  return cord shl SECTION_CELL_BITSHIFT
}

fun toCellId(worldX: Int, worldZ: Int): Long {
  val cx = toCell(worldX)
  val cz = toCell(worldZ)
  return Vectors.toChunkLong(Vector2i.from(cx, cz))
}

fun fromCellId(cellId: Long): Vector2i {
  val vec = Vectors.fromChunkLong(cellId)

  return vec
    .withX(toWorld(vec.x()))
    .withY(toWorld(vec.y()))
}

fun toCellCenter(cellId: Long): Vector2i {
  return fromCellId(cellId).add(SECTION_CELL_SIZE / 2, SECTION_CELL_SIZE / 2)
}

class SessionManager {
  private val byPlayerId: MutableMap<UUID, DungeonSession> = Object2ObjectOpenHashMap()
  private val byCellId: Long2ObjectMap<DungeonSession> = Long2ObjectOpenHashMap()
  private val usedCells: LongSet = LongOpenHashSet()

  private var tickTask: BukkitTask? = null

  fun close() {
    stopTicking()

    val nset = HashSet(byCellId.values)
    for (session in nset) {
      session.close()
    }
  }

  fun startTicking() {
    stopTicking()
    tickTask = Tasks.runTimer(this::tick, 1L, 1L)
  }

  fun stopTicking() {
    tickTask = Tasks.cancel(tickTask)
  }

  private fun tick() {
    for (value in byCellId.values) {
      value.tick()
    }
  }

  fun freeCell(cellId: Long) {
    byCellId.remove(cellId)
    usedCells.remove(cellId)
  }

  fun acquireCell(): Long {
    val cellId = findFreeCell()
    usedCells.add(cellId)
    return cellId
  }

  private fun findFreeCell(): Long {
    var cellX = 0
    var cellZ = 0
    var moveX = 1
    var moveZ = 0
    var segmentLength = 1
    var segmentPassed = 0
    var directionChanges = 0

    var cellId: Long = 0L

    while (true) {
      //Check cell state after movement, so 0,0 always remains free

      cellX += moveX
      cellZ += moveZ
      segmentPassed++

      cellId = Vectors.toChunkLong(cellX, cellZ)
      if (!usedCells.contains(cellId)) {
        return cellId
      }

      if (segmentPassed >= segmentLength) {
        segmentPassed = 0
        directionChanges++

        val temp = moveX
        moveX = -moveZ
        moveZ = temp

        if (directionChanges % 2 == 0) {
          segmentLength++
        }
      }
    }
  }

  fun getSession(player: Player): Optional<DungeonSession> {
    return Optional.ofNullable(byPlayerId[player.uniqueId])
  }

  internal fun onRemovePlayer(playerId: UUID) {
    byPlayerId.remove(playerId)
  }

  internal fun onAddPlayer(playerId: UUID, session: DungeonSession) {
    byPlayerId[playerId] = session
  }

  internal fun onSessionClose(session: DungeonSession) {
    freeCell(session.cellId)

    for (player in session.getPlayers()) {
      byPlayerId.remove(player.uniqueId)
    }
  }
}