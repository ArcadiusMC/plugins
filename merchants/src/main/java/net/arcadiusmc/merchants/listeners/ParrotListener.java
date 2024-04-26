package net.arcadiusmc.merchants.listeners;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.merchants.MerchantsPlugin;
import net.arcadiusmc.merchants.ParrotMerchant;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDismountEvent;

@RequiredArgsConstructor
public class ParrotListener implements Listener {

  private final MerchantsPlugin plugin;

  @EventHandler(ignoreCancelled = true)
  public void onEntityDismount(EntityDismountEvent event) {
    Entity entity = event.getEntity();
    Set<String> tags = entity.getScoreboardTags();

    if (!tags.contains(ParrotMerchant.SCOREBOARD_TAG)) {
      return;
    }

    event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getSpawnReason() != SpawnReason.SHOULDER_ENTITY) {
      return;
    }

    LivingEntity entity = event.getEntity();
    if (!(entity instanceof Parrot parrot)) {
      return;
    }

    if (!parrot.getScoreboardTags().contains(ParrotMerchant.SCOREBOARD_TAG)) {
      return;
    }

    AnimalTamer owner = parrot.getOwner();
    if (!(owner instanceof Player player)) {
      return;
    }

    event.setCancelled(true);

    player.getScheduler().runDelayed(
        plugin,
        scheduledTask -> player.setShoulderEntityLeft(entity),
        null,
        1
    );
  }
}
