package net.arcadiusmc.afk.listeners;

import net.arcadiusmc.afk.Afk;
import net.arcadiusmc.afk.PlayerAfkState;
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

  private final Afk afk;

  public AfkListener(Afk afk) {
    this.afk = afk;
  }

  private void checkUnafk(PlayerEvent event) {
    checkUnafk(event.getPlayer());
  }

  private void checkUnafk(Player player) {
    User user = Users.get(player);

    afk.getState(user).ifPresent(state -> {
      if (!state.isAfk()) {
        state.setAfkTicks(0);
        return;
      }

      state.unafk();
    });
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    afk.addEntry(event.getPlayer().getUniqueId());
  }

  @EventHandler(ignoreCancelled = true)
  public void onUserLeave(UserLeaveEvent event) {
    User user = event.getUser();

    afk.getState(user)
        .filter(PlayerAfkState::isAfk)
        .ifPresent(state -> {
          state.logAfkTime(user);
        });

    afk.removeEntry(user.getUniqueId());
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
