package net.arcadiusmc.core.listeners;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ServerPingListener implements Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
  public void onPaperServerListPing(PaperServerListPingEvent event) {
    var it = event.iterator();

    var service = Users.getService();
    if (!service.userLoadingAllowed()) {
      return;
    }

    while (it.hasNext()) {
      Player player = it.next();
      User user = Users.get(player);

      if (user.get(Properties.VANISHED)) {
        it.remove();
      }
    }
  }
}
