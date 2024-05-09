package net.arcadiusmc.ui;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class Sessions {

  private final Map<UUID, PlayerSession> byPlayerId = new Object2ObjectOpenHashMap<>();

  private BukkitTask task;

  public void startTask() {
    task = Tasks.runTimer(this::tick, 1, 1);
  }

  public void stopTask() {
    task = Tasks.cancel(task);
  }

  private void tick() {
    for (PlayerSession value : byPlayerId.values()) {
      value.tick();
    }
  }

  public Optional<PlayerSession> getSession(UUID playerId) {
    Objects.requireNonNull(playerId, "Null playerId");
    return Optional.ofNullable(byPlayerId.get(playerId));
  }

  public PlayerSession acquireSession(Player player) {
    Objects.requireNonNull(player, "Null player");

    UUID playerId = player.getUniqueId();
    return byPlayerId.computeIfAbsent(playerId, uuid -> new PlayerSession(player));
  }

  public void closeSession(UUID playerId) {
    Objects.requireNonNull(playerId, "Null playerId");

    PlayerSession session = byPlayerId.remove(playerId);
    if (session == null) {
      return;
    }

    session.kill();
  }

  public Collection<PlayerSession> getSessions() {
    return Collections.unmodifiableCollection(byPlayerId.values());
  }

  public void kill() {
    for (PlayerSession value : byPlayerId.values()) {
      value.kill();
    }
    byPlayerId.clear();
  }
}
