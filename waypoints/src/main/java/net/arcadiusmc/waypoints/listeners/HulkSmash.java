package net.arcadiusmc.waypoints.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.waypoints.WaypointPrefs;
import net.arcadiusmc.waypoints.event.WaypointVisitEvent;
import net.arcadiusmc.waypoints.event.WaypointVisitEvent.EventType;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
public class HulkSmash implements Listener {

  /**
   * Determines the amount of game ticks between cosmetic effect tick
   */
  public static final byte GAME_TICKS_PER_COSMETIC_TICK = 1;

  private static final Map<UUID, HulkSmash> listeners = new HashMap<>();

  private final User user;

  private boolean active = false;

  public static void startHulkSmash(User user) {
    HulkSmash listener = listeners.computeIfAbsent(user.getUniqueId(), uuid -> new HulkSmash(user));

    if (!listener.active) {
      listener.beginListening();
    }
  }

  public static void interrupt(User user) {
    Objects.requireNonNull(user);
    var smash = listeners.get(user.getUniqueId());

    if (smash != null) {
      smash.unregister(false);
    } else {
      user.set(WaypointPrefs.HULK_SMASHING, false);
    }
  }

  public void beginListening() {
    user.set(WaypointPrefs.HULK_SMASHING, true);
    active = true;

    Events.register(this);

    tickTask = Tasks.runTimer(
        this::tick,
        GAME_TICKS_PER_COSMETIC_TICK,
        GAME_TICKS_PER_COSMETIC_TICK
    );
  }

  private short ticks = 30 * (Ticks.TICKS_PER_SECOND / GAME_TICKS_PER_COSMETIC_TICK);
  private short groundTicks = 0;

  private BukkitTask tickTask;

  private void tick() {
    // Test if on ground, god-damn floating point errors, use some magic
    // floating point value that a player's Y velocity is always at, when
    // standing on the ground
    if (user.getPlayer().isOnGround()) {
      ++groundTicks;
    }

    // If been on ground for 5 or more ticks, stop
    if (groundTicks >= 5) {
      end();
      return;
    }

    // If below max fall tick, stop
    if (--ticks < 1) {
      unregister(true);
      return;
    }

    try {
      new WaypointVisitEvent(user, user.getLocation(), EventType.ON_TICK_DOWN).callEvent();
    } catch (Exception e) {
      unregister(true);
    }
  }

  public void unregister(boolean unsetProperty) {
    HandlerList.unregisterAll(this);

    if (unsetProperty) {
      user.set(WaypointPrefs.HULK_SMASHING, false);
    }

    listeners.remove(user.getUniqueId());
    active = false;

    tickTask = Tasks.cancel(tickTask);
  }

  private void end() {
    unregister(true);
    user.playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1);

    Particle.EXPLOSION.builder()
        .location(user.getLocation())
        .allPlayers()
        .count(5)
        .extra(0.0D)
        .offset(1, 1, 1)
        .spawn();

    new WaypointVisitEvent(user, user.getLocation(), EventType.ON_LAND).callEvent();
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (!user.getUniqueId().equals(event.getEntity().getUniqueId())) {
      return;
    }

    if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }

    event.setCancelled(true);
    end();
  }
}