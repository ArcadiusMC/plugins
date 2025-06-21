package net.arcadiusmc.dungeons

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.arcadiusmc.Loggers
import net.arcadiusmc.dungeons.gen.DungeonGenerator
import net.arcadiusmc.events.Events
import net.arcadiusmc.registry.Holder
import net.arcadiusmc.text.Messages
import net.arcadiusmc.text.Text
import net.arcadiusmc.text.TextJoiner
import net.arcadiusmc.user.Users
import net.arcadiusmc.utils.Tasks
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.spongepowered.math.vector.Vector3i

private val LOGGER = Loggers.getLogger()

class DungeonSession {
  val cellId: Long
  val cellMin: Vector3i

  val manager: SessionManager

  private val players: MutableSet<Player> = ObjectOpenHashSet()
  private val listener: SessionListener = SessionListener(this)

  var state: SessionState = SessionState.INACTIVE
  var tickCount: Int = 0

  var genParams: GenerationParameters = GenerationParameters()
  var levelType: Holder<LevelType>? = null

  constructor(manager: SessionManager, cellId: Long) {
    this.manager = manager
    this.cellId = cellId

    val cellMin = fromCellId(cellId)
    this.cellMin = Vector3i(cellMin.x(), 0, cellMin.y())
  }

  val displayName: Component
    get() {
      val cellPos = fromCellId(cellId)

      val baseName = Messages.render("dungeons.sessionDisplay.name")
        .addValue("playerCount", players.size)
        .addValue("cellX", cellPos.x())
        .addValue("cellZ", cellPos.y())
        .asComponent()

      val hoverText = Messages.render("dungeons.sessionDisplay.hover")
        .addValue("playerCount", players.size)
        .addValue("players",
          TextJoiner.onComma()
            .add(
              players.stream()
                .map { Users.get(it) }
                .map { it.displayName() }
            )
            .asComponent()
        )
        .addValue("cellX", cellPos.x())
        .addValue("cellZ", cellPos.y())
        .asComponent()

      return baseName.hoverEvent(hoverText)
    }

  fun getPlayers(): Set<Player> {
    return players;
  }

  fun addPlayer(player: Player) {
    if (!players.add(player)) {
      return
    }

    manager.onAddPlayer(player.uniqueId, this)
  }

  fun removePlayer(player: Player) {
    if (!players.remove(player)) {
      return
    }

    manager.onRemovePlayer(player.uniqueId)
  }

  fun start() {
    val levelType = this.levelType

    if (levelType == null) {
      LOGGER.error("Unknown level type for session {}", Text.plain(displayName))
      return
    }

    val cfg = DungeonConfig()
    val cellCenter = toCellCenter(cellId)

    levelType.value.applyTo(cfg)
    cfg.location = Vector3i(cellCenter.x(), 0, cellCenter.y())

    state = SessionState.GENERATING

    val random = cfg.createRandom()

    DungeonGenerator.generateAsync(cfg, random)
      .thenAccept {
        Tasks.runSync {
          onLevelGenerated(it)
        }
      }
  }

  private fun onLevelGenerated(gen: DungeonGenerator) {
    val world = DungeonWorld.get()
    val buffer = gen.buffer

    buffer.place(world).thenRun { onLevelPlaced(world, gen) }
  }

  private fun onLevelPlaced(world: World, gen: DungeonGenerator) {
    // Get spawn positions
    val playerSpawns = gen.getFunctions(LevelFunctions.PLAYER_SPAWNS)
    if (playerSpawns.isEmpty()) {
      LOGGER.warn("No player spawns! Cannot teleport players inside!! Session={}", cellId)
      return
    }

    // Teleport players inside
    var playerIt = players.iterator()
    for (playerSpawn in playerSpawns) {
      val pos = playerSpawn.position.toDouble().add(0.5, 0.0, 0.5)
      val loc = Location(world, pos.x(), pos.y(), pos.z())

      val player: Player

      if (playerIt.hasNext()) {
        player = playerIt.next()
      } else {
        playerIt = players.iterator()
        player = playerIt.next()
      }

      player.teleport(loc)
    }

    // Set to active state
    state = SessionState.ACTIVE
  }

  fun onSessionCreate() {
    Events.register(listener)
    state = SessionState.WAITING
  }

  fun tick() {
    tickCount++
  }

  fun close() {
    state = SessionState.INACTIVE
    Events.unregister(listener)
    manager.onSessionClose(this)
  }
}

enum class SessionState {
  INACTIVE,
  WAITING,
  GENERATING,
  ACTIVE
}

class SessionListener(val session: DungeonSession): Listener {

  @EventHandler(ignoreCancelled = true)
  fun onPlayerDeath(event: PlayerDeathEvent) {
    session.removePlayer(event.player)
  }

  @EventHandler(ignoreCancelled = true)
  fun onPlayerQuit(event: PlayerQuitEvent) {
    session.removePlayer(event.player)
  }
}