package net.arcadiusmc.afk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.scheduler.BukkitTask;

public final class Afk {

  public static final int TICK_INTERVAL = 20;

  private final AfkPlugin plugin;

  private final Map<UUID, PlayerAfkState> stateMap = new HashMap<>();

  private BukkitTask tickTask;

  public Afk(AfkPlugin plugin) {
    this.plugin = plugin;
  }

  public void addEntry(UUID playerId) {
    stateMap.computeIfAbsent(playerId, PlayerAfkState::new);
  }

  public void removeEntry(UUID playerId) {
    stateMap.remove(playerId);
  }

  public Optional<PlayerAfkState> getState(User user) {
    return Optional.ofNullable(stateMap.get(user.getUniqueId()));
  }

  public void startTicking() {
    stopTicking();
    tickTask = Tasks.runTimer(this::tick, TICK_INTERVAL, TICK_INTERVAL);
  }

  public void stopTicking() {
    tickTask = Tasks.cancel(tickTask);
  }

  private void tick() {
    AfkConfig config = plugin.getAfkConfig();

    if (!config.isAutoAfkEnabled()) {
      return;
    }

    for (PlayerAfkState value : stateMap.values()) {
      value.tick(config);
    }
  }
}
