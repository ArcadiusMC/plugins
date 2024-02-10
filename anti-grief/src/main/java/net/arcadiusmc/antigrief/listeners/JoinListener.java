package net.arcadiusmc.antigrief.listeners;

import net.arcadiusmc.antigrief.GriefPermissions;
import net.arcadiusmc.antigrief.PunishEntry;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.antigrief.StaffNote;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserJoinEvent;
import net.arcadiusmc.utils.Audiences;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class JoinListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onUserJoin(UserJoinEvent event) {
    User user = event.getUser();

    PunishEntry entry = Punishments.entry(user);
    assert entry != null;

    var notes = entry.getNotes();

    if (notes == null || notes.isEmpty()) {
      return;
    }

    var writer = TextWriters.newWriter();
    StaffNote.writeNotes(notes, writer, user);

    ChannelledMessage ch = ChannelledMessage.create(writer.asComponent());
    ch.setBroadcast();
    ch.filterTargets(audience -> {
      User viewer = Audiences.getUser(audience);

      if (viewer == null) {
        return false;
      }

      if (!viewer.hasPermission(GriefPermissions.PUNISH_NOTES)) {
        return false;
      }

      return viewer.get(StaffNote.VIEWS_NOTES);
    });
    ch.send();
  }
}
