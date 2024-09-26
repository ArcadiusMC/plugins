package net.arcadiusmc.pirates.catacombs

import net.arcadiusmc.pirates.getRandomSkeletonType
import net.arcadiusmc.pirates.riseFromTheGrave
import net.arcadiusmc.pirates.spawnSkeletonTypeAt
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.math.Vectors
import net.arcadiusmc.utils.math.WorldBounds3i
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d
import java.util.*

enum class CatacombState {
  INACTIVE,
  SPAWNED,
  AWAITING_DESPAWN
}

class Catacombs {
  val random: Random = Random()
  var state: CatacombState = CatacombState.INACTIVE

  var killTask: BukkitTask? = null
}

const val SKELETON_TAG = "blunderbeard.catacomb_guard"
const val DESPAWN_DELAY = 30L * 20L

private val spawns: List<Vector3d> = listOf(
  Vector3d(5554.5, 63.0, 1736.5),
  Vector3d(5557.5, 63.0, 1737.5),
  Vector3d(5541.5, 63.0, 1733.5),
  Vector3d(5543.5, 64.0, 1744.5),
  Vector3d(5537.5, 65.0, 1752.5),
  Vector3d(5534.5, 65.0, 1754.5),
  Vector3d(5552.5, 64.0, 1750.5),
  Vector3d(5560.5, 64.0, 1743.5),
  Vector3d(5550.5, 65.0, 1767.5),
  Vector3d(5553.5, 65.0, 1764.5),
  Vector3d(5545.5, 64.0, 1720.5)
)

private val currentState = Catacombs()

// Called with a script from the usable trigger that
// encapsulates the catacomb entry area
fun spawn(world: World) {
  if (currentState.state == CatacombState.SPAWNED) {
    return
  }

  if (currentState.state == CatacombState.INACTIVE) {
    for (spawn in spawns) {
      val pitch = currentState.random.nextFloat(-22.5f, 22.5f)
      val yaw = currentState.random.nextFloat(-180f, 180f)

      val location = Location(world, spawn.x, spawn.y - 2.0, spawn.z, yaw, pitch)
      val type = getRandomSkeletonType(currentState.random)

      val skeleton = spawnSkeletonTypeAt(type, location, currentState.random)

      skeleton.removeWhenFarAway = false
      skeleton.isPersistent = true
      skeleton.setShouldBurnInDay(false)
      skeleton.addScoreboardTag(SKELETON_TAG)

      riseFromTheGrave(skeleton)
    }
  }

  currentState.state = CatacombState.SPAWNED

  if (Tasks.isScheduled(currentState.killTask)) {
    Tasks.cancel(currentState.killTask)
    currentState.killTask = null
  }
}

// Called in the same way as the above method
fun onLeaveArea(boundingBox: WorldBounds3i) {
  Tasks.runLater({
    val players = boundingBox.players
    if (!players.isEmpty()) {
      return@runLater
    }

    val task = Tasks.runLater(KillTask(boundingBox), DESPAWN_DELAY)

    currentState.killTask = task
    currentState.state = CatacombState.AWAITING_DESPAWN
  }, 1)
}

private class KillTask(val boundingBox: WorldBounds3i): Runnable {

  override fun run() {
    killEntities()

    currentState.state = CatacombState.INACTIVE
    currentState.killTask = null
  }

  fun killEntities() {
    val a = 1 shr 2

    val chunkBounds = boundingBox.set(
      Vectors.toChunk(boundingBox.minX()),
      Vectors.toChunk(boundingBox.minY()),
      Vectors.toChunk(boundingBox.minZ()),
      Vectors.toChunk(boundingBox.maxX()),
      Vectors.toChunk(boundingBox.maxY()),
      Vectors.toChunk(boundingBox.maxZ())
    )

    val world = boundingBox.world

    for (x in chunkBounds.minX() until chunkBounds.maxX()) {
      for (z in chunkBounds.minZ() until chunkBounds.maxZ()) {
        val chunk = world.getChunkAt(x, z)
        val entities = chunk.entities

        for (entity in entities) {
          if (!entity.scoreboardTags.contains(SKELETON_TAG)) {
            continue
          }

          entity.remove()
        }
      }
    }
  }
}