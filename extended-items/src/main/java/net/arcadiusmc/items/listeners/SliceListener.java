package net.arcadiusmc.items.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.VanillaAccess;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

public class SliceListener implements Listener {

  private static final Logger LOGGER = Loggers.getLogger();

  static final long DELAY = 4;
  static final Set<String> alreadyAttacking = new HashSet<>();

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) {
      return;
    }

    ItemStack item = player.getInventory().getItemInMainHand();
    if (ItemStacks.isEmpty(item)) {
      return;
    }

    int level = item.getEnchantmentLevel(ArcadiusEnchantments.SLICE);
    if (level < 1) {
      return;
    }

    Entity entity = event.getEntity();
    String attackingId = player.getUniqueId() + "::" + entity.getUniqueId();

    if (alreadyAttacking.contains(attackingId)) {
      return;
    }

    AttackAgainTask task = new AttackAgainTask(
        player,
        entity,
        level,
        attackingId,
        VanillaAccess.getAttackTicker(player)
    );

    Tasks.runTimer(task, DELAY, DELAY);
  }

  class AttackAgainTask implements Consumer<BukkitTask> {

    final Player player;
    final Entity entity;
    final int repeatTarget;
    final String attackingId;
    final int strengthTicker;

    int repeats = 0;

    public AttackAgainTask(Player player, Entity entity, int repeatTarget, String attackingId, int strengthTicker) {
      this.player = player;
      this.entity = entity;
      this.repeatTarget = repeatTarget;
      this.attackingId = attackingId;
      this.strengthTicker = strengthTicker;
    }

    @Override
    public void accept(BukkitTask bukkitTask) {
      if (repeats >= repeatTarget) {
        return;
      }

      alreadyAttacking.add(attackingId);

      try {
        attack();
      } finally {
        alreadyAttacking.remove(attackingId);
        repeats++;

        if (repeats > repeatTarget || entity.isDead()) {
          bukkitTask.cancel();
        }
      }
    }

    void attack() {
      VanillaAccess.setInvulnerableTime(entity, 0);
      //VanillaAccess.setAttackTicker(player, strengthTicker);
      player.attack(entity);
    }
  }
}
