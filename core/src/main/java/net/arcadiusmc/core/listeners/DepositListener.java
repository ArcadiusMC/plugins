package net.arcadiusmc.core.listeners;

import com.google.common.collect.Iterators;
import net.arcadiusmc.core.Coins;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Cooldown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class DepositListener implements Listener {

  private final CorePlugin plugin;

  public DepositListener(CorePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND || !plugin.getCoreConfig().rightClickDeposits()) {
      return;
    }

    var held = event.getItem();
    var player = event.getPlayer();

    if (!Coins.isCoin(held)) {
      return;
    }

    if (Cooldown.containsOrAdd(player, "coin_deposit", 5)) {
      return;
    }

    int limit = player.isSneaking() ? -1 : 1;
    User user = Users.get(player);

    Coins.deposit(user, Iterators.singletonIterator(held), limit);
  }
}
