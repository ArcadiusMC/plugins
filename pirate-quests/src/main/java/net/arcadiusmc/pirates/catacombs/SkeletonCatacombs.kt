package net.arcadiusmc.pirates.catacombs

import net.arcadiusmc.pirates.getRandomSkeletonType
import net.arcadiusmc.pirates.riseFromTheGrave
import net.arcadiusmc.pirates.spawnSkeletonTypeAt
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.math.WorldBounds3i
import org.bukkit.Location
import org.bukkit.World
import org.joml.Vector3d
import java.util.*

class CatacombState {
  val random: Random = Random()
  var spawned: Boolean = false
}

const val SKELETON_TAG = "blunderbeard.catacomb_guard"

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

private val currentState = CatacombState()

fun spawn(world: World) {
  if (currentState.spawned) {
    return
  }

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

  currentState.spawned = true
}

fun onLeaveArea(boundingBox: WorldBounds3i) {
  Tasks.runLater({
    val players = boundingBox.players
    if (!players.isEmpty()) {
      return@runLater
    }

    currentState.spawned = false

    val entities = boundingBox.getEntities {
      return@getEntities it.scoreboardTags.contains(SKELETON_TAG)
    }

    for (entity in entities) {
      entity.remove()
    }
  }, 1)
}