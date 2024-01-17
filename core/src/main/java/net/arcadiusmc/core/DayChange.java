package net.arcadiusmc.core;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.events.DayChangeEvent;
import net.arcadiusmc.text.PeriodFormat;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

public class DayChange implements Runnable {

  public static final Logger LOGGER = Loggers.getLogger();

  private BukkitTask task;

  public void schedule() {
    stop();

    LocalTime resetTime = CorePlugin.plugin().getFtcConfig().dayUpdateTime();

    ZonedDateTime now = ZonedDateTime.now();
    LocalTime currentTime = now.toLocalTime();

    ZonedDateTime resetDate;

    if (currentTime.isBefore(resetTime)) {
      resetDate = now.with(resetTime);
    } else {
      resetDate = now.plusDays(1).with(resetTime);
    }

    Duration dur = Duration.between(now, resetDate);
    task = Tasks.runTimer(this, dur, Duration.ofDays(1));

    LOGGER.debug("Executing in {}", PeriodFormat.of(dur));
  }

  public void stop() {
    task = Tasks.cancel(task);
  }

  @Override
  public void run() {
    ZonedDateTime time = ZonedDateTime.now();
    DayChangeEvent event = new DayChangeEvent(time);
    event.callEvent();
  }
}
