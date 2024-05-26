package net.arcadiusmc.items.listeners;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.arcadiusmc.items.CallbackComponent;
import net.arcadiusmc.items.ItemTypes;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemCallbackListeners implements Listener {

  static final Set<EquipmentSlot> SLOTS = Set.of(
      EquipmentSlot.HEAD,
      EquipmentSlot.CHEST,
      EquipmentSlot.LEGS,
      EquipmentSlot.FEET,
      EquipmentSlot.HAND,
      EquipmentSlot.OFF_HAND
  );

  @EventHandler(ignoreCancelled = true)
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    Item item = event.getItem();
    performItemAction(item, component -> component.onPickup(event));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Item item = event.getItemDrop();
    performItemAction(item, component -> component.onDrop(event));
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    performActionOnAllSlots(event.getPlayer(), (c, slot) -> c.onInteractBlock(event, slot));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    performActionOnAllSlots(event.getPlayer(), (c, slot) -> c.onInteractEntity(event, slot));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    // ArmorStands for some reason don't trigger the normal interaction event
    if (!(event.getRightClicked() instanceof ArmorStand)) {
      return;
    }

    onPlayerInteractEntity(event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    Entity damager = event.getDamager();
    if (!(damager instanceof Player player)) {
      return;
    }

    performActionOnAllSlots(player, (c, slot) -> c.onAttack(player, event, slot));
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    performActionOnAllSlots(event.getPlayer(), (c, slot) -> c.onMineBlock(event, slot));
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    performActionOnAllSlots(player, (c, slot) -> c.onHolderDamaged(player, event, slot));
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    if (event.getEntity() instanceof Player player) {
      performActionOnAllSlots(player, (c, slot) -> c.onHolderDeath(player, event, slot));
    }

    if (event.getDamageSource().getCausingEntity() instanceof Player player) {
      performActionOnAllSlots(player, (c, slot) -> c.onKill(player, event, slot));
    }
  }

  private void performActionOnAllSlots(Player player, BiConsumer<CallbackComponent, EquipmentSlot> consumer) {
    PlayerInventory inventory = player.getInventory();

    for (EquipmentSlot slot : SLOTS) {
      ItemStack item = inventory.getItem(slot);
      performItemAction(item, component -> consumer.accept(component, slot));
    }
  }

  private void performItemAction(Item item, Consumer<CallbackComponent> consumer) {
    performItemAction(item.getItemStack(), consumer);
  }

  private void performItemAction(ItemStack stack, Consumer<CallbackComponent> consumer) {
    ItemTypes.getItem(stack)
        .ifPresent(item1 -> {
          List<CallbackComponent> list = item1.getMatching(CallbackComponent.class);

          if (list.isEmpty()) {
            return;
          }

          for (CallbackComponent c : list) {
            consumer.accept(c);
          }

          item1.update();
        });
  }
}
