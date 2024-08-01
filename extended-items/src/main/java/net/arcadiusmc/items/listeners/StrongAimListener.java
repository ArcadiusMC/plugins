package net.arcadiusmc.items.listeners;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.function.Consumer;
import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ItemPlugin;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StrongAimListener implements Listener {

  static final float FORCE = 1.10f;
  static final int UPDATE_INTERVAL = 3;

  private final ItemPlugin plugin;

  public StrongAimListener(ItemPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityShootBow(EntityShootBowEvent event) {
    ItemStack bow = event.getBow();
    if (ItemStacks.isEmpty(bow)) {
      return;
    }

    int level = bow.getEnchantmentLevel(ArcadiusEnchantments.STRONG_AIM);
    if (level < 1) {
      return;
    }

    Entity projectile = event.getProjectile();
    if (!(projectile instanceof Arrow arrow)) {
      return;
    }

    StrongAimTask task = new StrongAimTask(arrow);
    projectile.getScheduler().runAtFixedRate(plugin, task, null, UPDATE_INTERVAL, UPDATE_INTERVAL);
  }

  record StrongAimTask(Arrow arrow) implements Consumer<ScheduledTask> {

    @Override
    public void accept(ScheduledTask scheduledTask) {
      if (arrow.isDead() || arrow.isOnGround()) {
        scheduledTask.cancel();
        return;
      }

      Vector vel = arrow.getVelocity();
      vel.add(new Vector(0, FORCE, 0));

      arrow.setVelocity(vel);
    }
  }
}
