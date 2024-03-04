package net.arcadiusmc.waypoints.listeners;

import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.waypoints.WaypointPrefs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class PlayerJoinListener implements Listener {


  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    User user = Users.get(event.getPlayer());
    boolean hulkSmashing = user.get(WaypointPrefs.HULK_SMASHING);

    if (hulkSmashing) {
      HulkSmash.startHulkSmash(user);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    User user = Users.get(event.getPlayer());

    if (user.get(WaypointPrefs.HULK_SMASHING)) {
      HulkSmash.interrupt(user);
    }
  }
}
