package net.arcadiusmc.items;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public interface CallbackComponent {

  default void onAttack(Player player, EntityDamageByEntityEvent event, EquipmentSlot slot) {

  }

  default void onKill(Player player, EntityDeathEvent event, EquipmentSlot slot) {

  }

  default void onHolderDamaged(Player player, EntityDamageEvent event, EquipmentSlot slot) {

  }

  default void onMineBlock(BlockBreakEvent event, EquipmentSlot slot) {

  }

  default void onInteractEntity(PlayerInteractEntityEvent event, EquipmentSlot slot) {

  }

  default void onInteractBlock(PlayerInteractEvent event, EquipmentSlot slot) {

  }

  default void onDrop(PlayerDropItemEvent event) {

  }

  default void onPickup(EntityPickupItemEvent event) {

  }

  default void onHolderDeath(Player player, EntityDeathEvent event, EquipmentSlot slot) {

  }
}
