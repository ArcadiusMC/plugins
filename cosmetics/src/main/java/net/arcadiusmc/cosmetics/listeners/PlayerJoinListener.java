package net.arcadiusmc.cosmetics.listeners;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserJoinEvent;
import net.arcadiusmc.utils.MonthDayPeriod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerJoinListener implements Listener {

  static final GrantedPermission[] PERMISSIONS = {
      new GrantedPermission(
          MonthDayPeriod.between(MonthDay.of(Month.DECEMBER, 1), MonthDay.of(Month.DECEMBER, 31)),
          "arcadius.cosmetics.emotes.jingle",
          "cosmetics.emotes.granted.jingle"
      ),
      new GrantedPermission(
          MonthDayPeriod.between(MonthDay.of(Month.OCTOBER, 1), MonthDay.of(Month.OCTOBER, 31)),
          "arcadius.cosmetics.emotes.scare",
          "cosmetics.emotes.granted.scare"
      )
  };

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    User user = event.getUser();
    LocalDate date = LocalDate.now();

    for (GrantedPermission granted : PERMISSIONS) {
      if (!granted.period.contains(date)) {
        continue;
      }
      if (user.hasPermission(granted.permission)) {
        continue;
      }

      user.setPermission(granted.permission, true);
      user.sendMessage(Messages.renderText(granted.messageKey, user));
    }
  }

  record GrantedPermission(MonthDayPeriod period, String permission, String messageKey) {


  }
}
