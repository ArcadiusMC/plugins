package net.arcadiusmc.pirates.catacombs

import net.arcadiusmc.Worlds
import net.arcadiusmc.pirates.copyBlocks
import net.arcadiusmc.pirates.getRandomSkeletonType
import net.arcadiusmc.pirates.riseFromTheGrave
import net.arcadiusmc.pirates.spawnSkeletonTypeAt
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.math.Vectors
import net.arcadiusmc.utils.math.WorldBounds3i
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d
import org.spongepowered.math.vector.Vector3i
import java.lang.System.currentTimeMillis
import java.util.*

const val SKELETON_TAG = "blunderbeard.catacomb_guard"
const val DESPAWN_DELAY = 30L * 20L
const val TIME_BEFORE_RESPAWN_AFTER_DEFEAT = 5L * 60L * 1000L

private val DOOR_PASTE = Vector3i.from(5532, 64, 1786)
private val OPEN_ORIGIN_MIN = Vector3i.from(5499, 119, 1879)
private val OPEN_ORIGIN_MAX = Vector3i.from(5512, 133, 1884)
private val CLOSED_ORIGIN_MIN = Vector3i.from(5499, 119, 1886)
private val CLOSED_ORIGIN_MAX = Vector3i.from(5512, 133, 1891)

private val SPAWNS: List<Vector3d> = listOf(
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
fun onEnterCatacombRegion(boundingBox: WorldBounds3i, player: Player) {
  if (currentState.boundingBox == null) {
    currentState.boundingBox = boundingBox
  }

  if (Tasks.isScheduled(currentState.killTask)) {
    Tasks.cancel(currentState.killTask)
    currentState.killTask = null
  }

  if (currentState.state == CatacombState.DEFEATED) {
    val time = System.currentTimeMillis()
    val nextAllowedSpawn = currentState.defeatTime + TIME_BEFORE_RESPAWN_AFTER_DEFEAT

    // If after next allowed spawn
    if (time > nextAllowedSpawn) {
      spawn(boundingBox, player)
    } else {
      currentState.bossbar.addPlayer(player)
    }

    return
  }

  if (currentState.state == CatacombState.SPAWNED) {
    currentState.bossbar.addPlayer(player)
    return
  }

  if (currentState.state == CatacombState.INACTIVE) {
    spawn(boundingBox, player)
  }
}

private fun spawn(boundingBox: WorldBounds3i, player: Player) {
  val world = boundingBox.world

  currentState.totalSpawnedHealth = 0.0

  for (spawn in SPAWNS) {
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

    currentState.skeletons.add(skeleton)
    currentState.totalSpawnedHealth += skeleton.health
  }

  currentState.originallySpawnedCount = currentState.skeletons.size

  val bar = currentState.bossbar
  bar.color = BarColor.RED
  bar.progress = 1.0
  bar.isVisible = true
  bar.setTitle("Remaining Skeletons (${currentState.skeletons.size})")
  bar.addPlayer(player)

  closeGate()

  currentState.state = CatacombState.SPAWNED
}

// Called in the same way as the above onEnter method
fun onLeaveCatacombRegion(boundingBox: WorldBounds3i, player: Player) {
  currentState.bossbar.removePlayer(player)

  if (currentState.state == CatacombState.DEFEATED) {
    return
  }

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

private fun closeGate() {
  val world = currentState.boundingBox?.world ?: return

  copyBlocks(
    CLOSED_ORIGIN_MIN,
    CLOSED_ORIGIN_MAX,
    DOOR_PASTE,
    world
  )
}

private fun openGate() {
  val world = currentState.boundingBox?.world ?: return

  copyBlocks(
    OPEN_ORIGIN_MIN,
    OPEN_ORIGIN_MAX,
    DOOR_PASTE,
    world
  )
}

private class KillTask(val boundingBox: WorldBounds3i): Runnable {

  override fun run() {
    killEntities()

    currentState.state = CatacombState.INACTIVE
    currentState.killTask = null

    currentState.bossbar.removeAll()
    currentState.bossbar.isVisible = false
  }

  fun killEntities() {
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

class CatacombListener: Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  fun onEntityDeath(event: EntityDeathEvent) {
    val entity = event.entity
    val changed = currentState.skeletons.removeIf { it.uniqueId == entity.uniqueId }

    if (!changed) {
      return
    }

    val bar = currentState.bossbar

    if (currentState.skeletons.isEmpty()) {
      bar.color = BarColor.BLUE
      bar.progress = 1.0
      bar.setTitle("All enemies defeated, enter the temple")

      currentState.state = CatacombState.DEFEATED
      currentState.defeatTime = currentTimeMillis()

      openGate()
      return
    }

    if (currentState.skeletons.size < (currentState.originallySpawnedCount / 2)) {
      for (skeleton in currentState.skeletons) {
        skeleton.isGlowing = true
      }
    }

    bar.setTitle("Remaining Skeletons (${currentState.skeletons.size})")
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  fun onEntityDamage(event: EntityDamageEvent) {
    val entity = event.entity
    val idx = currentState.skeletons.indexOfFirst { it.uniqueId == entity.uniqueId }

    if (idx < 0) {
      return
    }

    val maxHealth = currentState.totalSpawnedHealth
    var health = 0.0

    for (skeleton in currentState.skeletons) {
      if (skeleton.uniqueId == entity.uniqueId) {
        val h = skeleton.health
        var newHealth = h - event.finalDamage

        if (newHealth <= 0) {
          newHealth = 0.0
        }

        health += newHealth
        continue
      }

      health += skeleton.health
    }

    val prog = health / maxHealth
    currentState.bossbar.progress = prog
  }

  @EventHandler
  fun onPlayerLeave(event: PlayerQuitEvent) {
    val bb = currentState.boundingBox ?: return

    if (!bb.contains(event.player)) {
      return
    }

    val exitPos = Location(Worlds.overworld(), 2834.5, 64.0, 1321.5, -135f, 0f)
    event.player.teleport(exitPos)
  }
}

enum class CatacombState {
  INACTIVE,
  SPAWNED,
  AWAITING_DESPAWN,
  DEFEATED
}

class Catacombs {
  val random: Random = Random()
  val skeletons: MutableList<Skeleton> = ArrayList()

  var state: CatacombState = CatacombState.INACTIVE
  var totalSpawnedHealth: Double = 0.0
  var originallySpawnedCount: Int = 0
  var boundingBox: WorldBounds3i? = null

  var defeatTime: Long = 0L

  var killTask: BukkitTask? = null

  private var internalBossBar: BossBar? = null

  val bossbar: BossBar
    get() {
      if (internalBossBar == null) {
        internalBossBar = Bukkit.createBossBar(
          "Remaining Skeletons",
          BarColor.RED,
          BarStyle.SOLID,
          BarFlag.DARKEN_SKY
        )
      }

      return internalBossBar!!
    }
}