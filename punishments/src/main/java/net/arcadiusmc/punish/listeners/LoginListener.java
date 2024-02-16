package net.arcadiusmc.punish.listeners;

import com.google.common.base.Strings;
import java.util.UUID;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishManager;
import net.arcadiusmc.punish.PunishType;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.text.DefaultTextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

class LoginListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    var msg = testBanned(event.getUniqueId());

    if (msg != null) {
      event.disallow(Result.KICK_BANNED, msg);
    }
  }

  private Component testBanned(UUID userId) {
    var alts = Users.getService();
    var accounts = alts.getOtherAccounts(userId);
    accounts.add(userId);

    PunishManager punishments = Punishments.getManager();

    for (var accountId: accounts) {
      PunishEntry entry = punishments.getEntry(accountId);

      Component msg = banMessage(entry, PunishType.BAN);
      if (msg != null) {
        return msg;
      }

      Component ipMsg = banMessage(entry, PunishType.IPBAN);
      if (ipMsg != null) {
        return ipMsg;
      }
    }

    return null;
  }

  private Component banMessage(PunishEntry entry, PunishType type) {
    Punishment punishment = entry.getCurrent(type);
    if (punishment == null) {
      return null;
    }

    DefaultTextWriter writer = TextWriters.newWriter();
    writer.line("You are banned!");

    if (!Strings.isNullOrEmpty(punishment.getReason())) {
      writer.line(punishment.getReason());
      writer.newLine();
      writer.newLine();
    }

    if (punishment.getExpires() != null) {
      writer.formattedField("Expires",
          "{0, date} (in {0, time, -timestamp})",
          punishment.getExpires()
      );
    }

    // writer.formattedLine("Source: {0}", punishment.getSource());
    return writer.asComponent();
  }
}