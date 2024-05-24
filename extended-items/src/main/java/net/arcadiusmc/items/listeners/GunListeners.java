package net.arcadiusmc.items.listeners;

import static net.arcadiusmc.items.guns.ProjectileGun.YIELD_KEY;

import com.sk89q.worldedit.math.BlockVector3;
import net.arcadiusmc.items.guns.BlockDamage;
import net.arcadiusmc.items.guns.PlayerMoveSpeeds;
import net.arcadiusmc.utils.math.WorldBounds3i;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class GunListeners implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    PlayerMoveSpeeds.SPEEDS.addPlayer(player);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    PlayerMoveSpeeds.SPEEDS.removePlayer(player);
  }

  @EventHandler(ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    processExplosion(event.getEntity());
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    processExplosion(event.getEntity());
  }

  private void processExplosion(Entity cause) {
    var pdc = cause.getPersistentDataContainer();

    if (!pdc.has(YIELD_KEY, PersistentDataType.FLOAT)) {
      return;
    }

    float yield = pdc.get(YIELD_KEY, PersistentDataType.FLOAT);
    Location loc = cause.getLocation();

    WorldBounds3i bounds = WorldBounds3i.of(loc, (int) yield + 1);
    BlockVector3 center = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());

    float maxDistSq = yield * yield;

    for (Block exploded : bounds) {
      BlockVector3 blockPos = BlockVector3.at(exploded.getX(), exploded.getY(), exploded.getZ());
      double distSq = center.distanceSq(blockPos);

      if (distSq >= maxDistSq) {
        continue;
      }

      BlockDamage.BLOCK_DAMAGE.damage(exploded, yield * 2);
    }
  }
}
