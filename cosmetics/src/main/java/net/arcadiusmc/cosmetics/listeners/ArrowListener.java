package net.arcadiusmc.cosmetics.listeners;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.function.Consumer;
import net.arcadiusmc.cosmetics.ActiveMap;
import net.arcadiusmc.cosmetics.ArrowEffects;
import net.arcadiusmc.cosmetics.Cosmetic;
import net.arcadiusmc.cosmetics.CosmeticsPlugin;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class ArrowListener implements Listener {

  private final CosmeticsPlugin plugin;

  public ArrowListener(CosmeticsPlugin plugin) {
    this.plugin = plugin;
  }


  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerLaunchProjectile(EntityShootBowEvent event) {
    Entity projectile = event.getProjectile();
    if (!(projectile instanceof Arrow arrow)) {
      return;
    }
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    ActiveMap map = plugin.getActiveMap();
    Cosmetic<Particle> effect = map.getActive(player.getUniqueId(), ArrowEffects.type);

    if (effect == null) {
      return;
    }

    EntityScheduler scheduler = arrow.getScheduler();
    scheduler.runAtFixedRate(
        plugin,
        new ArrowEffectTask(player, arrow, effect.getValue()),
        null,
        1,
        1
    );
  }

  record ArrowEffectTask(Player player, Arrow arrow, Particle particle)
      implements Consumer<ScheduledTask>
  {

    @Override
    public void accept(ScheduledTask scheduledTask) {
      if (arrow.isOnGround() || arrow.isDead()) {
        scheduledTask.cancel();
        return;
      }

      particle.builder()
          .location(arrow.getLocation())
          .source(player)
          .allPlayers()
          .extra(0)
          .spawn();
    }
  }
}
