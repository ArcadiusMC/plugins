package net.arcadiusmc.bank;

import net.arcadiusmc.events.CoinpileCollectEvent;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BankListener implements Listener {

  private BankPlugin plugin;

  public BankListener(BankPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onCoinpileCollect(CoinpileCollectEvent event) {
    Player player = event.getPlayer();
    BankRun bankRun = plugin.getSessionMap().get(player.getUniqueId());

    if (bankRun == null) {
      return;
    }

    bankRun.setPickedCoins(bankRun.getPickedCoins() + event.getCoinValue());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    BankRun bankRun = plugin.getSessionMap().get(player.getUniqueId());

    if (bankRun == null) {
      return;
    }

    bankRun.kick(true);
    event.setCancelled(true);

    Tasks.runLater(() -> {
      Location exit = bankRun.getVault()
          .getExitPosition()
          .toLocation(bankRun.getWorld());

      player.teleport(exit);
    }, 1);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    BankRun bankRun = plugin.getSessionMap().get(player.getUniqueId());

    if (bankRun == null) {
      return;
    }

    bankRun.kick(true);
  }
}
