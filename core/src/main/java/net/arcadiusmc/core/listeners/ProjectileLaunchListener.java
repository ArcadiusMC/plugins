package net.arcadiusmc.core.listeners;

import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

public class ProjectileLaunchListener implements Listener {

  private static final String TAG = "no_projectile_launches";

  @EventHandler(ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    Projectile proj = event.getEntity();
    ProjectileSource shooter = proj.getShooter();

    if (!(shooter instanceof Entity shooterEntity)) {
      return;
    }

    Set<String> tags = shooterEntity.getScoreboardTags();
    if (!tags.contains(TAG)) {
      return;
    }

    event.setCancelled(true);
  }
}
