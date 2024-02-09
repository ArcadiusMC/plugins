package net.arcadiusmc.sellshop.listeners;

import net.arcadiusmc.sellshop.UserShopData;
import net.arcadiusmc.user.event.UserJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class PlayerJoinListener implements Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onUserJoin(UserJoinEvent event) {
    var user = event.getUser();
    var data = user.getComponent(UserShopData.class);
    data.onLogin();
  }
}
