package net.arcadiusmc.items.guns;

import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public abstract class TickingObject {

  private BukkitTask task;

  public void stop() {
    task = Tasks.cancel(task);
  }

  public void start() {
    stop();

    BukkitScheduler scheduler = Bukkit.getScheduler();
    task = scheduler.runTaskTimer(PluginUtil.getPlugin(), this::tick, 1, 1);
  }

  public boolean isTicking() {
    return Tasks.isScheduled(task);
  }

  public abstract void tick();
}
