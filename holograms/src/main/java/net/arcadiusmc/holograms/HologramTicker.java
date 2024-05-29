package net.arcadiusmc.holograms;

import java.util.Collection;
import java.util.Optional;
import net.arcadiusmc.utils.Tasks;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

public class HologramTicker {

  static final int DELAY = Ticks.TICKS_PER_SECOND;

  private final ServiceImpl service;
  private BukkitTask task;

  public HologramTicker(ServiceImpl service) {
    this.service = service;
  }

  public void startTicking() {
    stopTicking();
    task = Tasks.runTimer(this::tick, DELAY, DELAY);
  }

  public void stopTicking() {
    task = Tasks.cancel(task);
  }

  private void tick() {
    updatePositions(service.getBoards().values());
    updatePositions(service.getTexts().values());
  }

  private void updatePositions(Collection<? extends Hologram> collection) {
    Location loc = new Location(null, 0, 0, 0);

    for (Hologram hologram : collection) {
      if (!hologram.isSpawned()) {
        continue;
      }

      Optional<TextDisplay> opt = hologram.getEntity();

      if (opt.isEmpty()) {
        continue;
      }

      TextDisplay gotten = opt.get();
      gotten.getLocation(loc);

      if (loc.equals(hologram.getLocation())) {
        continue;
      }

      hologram.setLocation(loc, false);
    }
  }
}
