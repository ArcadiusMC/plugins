package net.arcadiusmc.dungeons

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.arcadiusmc.events.Events
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.spongepowered.math.vector.Vector3i

class DungeonSession {
  val cellId: Long
  val cellMin: Vector3i

  val manager: SessionManager

  private val players: MutableSet<Player> = ObjectOpenHashSet()
  private val listener: SessionListener = SessionListener(this)

  var state: SessionState = SessionState.INACTIVE
  var tickCount: Int = 0

  constructor(manager: SessionManager, cellId: Long) {
    this.manager = manager
    this.cellId = cellId

    val cellMin = fromCellId(cellId)
    this.cellMin = Vector3i(cellMin.x(), 0, cellMin.y())
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

  }

  @EventHandler(ignoreCancelled = true)
  fun onPlayerQuit(event: PlayerQuitEvent) {

  }
}