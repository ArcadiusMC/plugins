package net.arcadiusmc.cosmetics.listeners;

import net.arcadiusmc.cosmetics.ActiveMap;
import net.arcadiusmc.cosmetics.Cosmetic;
import net.arcadiusmc.cosmetics.CosmeticsPlugin;
import net.arcadiusmc.cosmetics.DeathEffect;
import net.arcadiusmc.cosmetics.DeathEffects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

  private final CosmeticsPlugin plugin;

  public DeathListener(CosmeticsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    ActiveMap map = plugin.getActiveMap();

    Cosmetic<DeathEffect> active = map.getActive(player.getUniqueId(), DeathEffects.type);

    if (active == null) {
      return;
    }

    active.getValue().activate(player.getLocation());
  }
}
