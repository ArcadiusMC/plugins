package net.arcadiusmc.utils.collision;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public class PlayerCollisionSystem<T> extends CollisionSystem<Player, T> {

  protected PlayerCollisionSystem(
      CollisionLookup<T> map,
      Plugin plugin,
      CollisionListener<Player, T> listener
  ) {
    super(map, plugin, listener);
  }

  @Override
  protected Listener makeListener() {
    return new PlayerListener(this);
  }
}

@RequiredArgsConstructor
class PlayerListener implements Listener {
  private final PlayerCollisionSystem<?> system;

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    system.run(event.getPlayer(), true);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onPlayerQuit(PlayerQuitEvent event) {
    system.run(event.getPlayer(), false);
  }

  private void onMove(PlayerMoveEvent event, boolean teleport) {
    if (!event.hasChangedPosition()) {
      return;
    }

    system.run(event.getPlayer(), event.getFrom(), event.getTo(), teleport);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    onMove(event, false);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    onMove(event, true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerTeleportEndGateway(PlayerTeleportEndGatewayEvent event) {
    onMove(event, true);
  }
}
