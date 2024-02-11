package net.arcadiusmc.afk.listeners;

import net.arcadiusmc.afk.Afk;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.event.UserJoinEvent;
import net.arcadiusmc.user.event.UserLeaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AfkListener implements Listener {

  public static void checkUnafk(PlayerEvent event) {
    checkUnafk(event.getPlayer());
  }

  public static void checkUnafk(Player player) {
    User user = Users.get(player);

    if (!Afk.isAfk(user)) {
      Afk.delayAutoAfk(user);
      return;
    }

    Afk.unafk(user);
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    Afk.setAfk(event.getUser(), false, null);
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserLeave(UserLeaveEvent event) {
    User user = event.getUser();

    Afk.getState(user).ifPresent(afkState -> {
      if (afkState.isAfk()) {
        Afk.logAfkTime(user);
      }

      afkState.cancelAutoAfk();
      afkState.cancelPunishTask();
    });
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedOrientation()) {
      return;
    }

    checkUnafk(event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (!event.getMessage().startsWith("/afk") && !event.getMessage().startsWith("afk")) {
      checkUnafk(event);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    checkUnafk(event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    checkUnafk(event.getEntity());
  }
}
