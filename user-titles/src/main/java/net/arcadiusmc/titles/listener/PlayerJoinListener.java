package net.arcadiusmc.titles.listener;

import net.arcadiusmc.titles.UserTitles;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    User user = Users.get(event.getPlayer());
    UserTitles titles = user.getComponent(UserTitles.class);
    titles.ensureSynced();
  }
}
