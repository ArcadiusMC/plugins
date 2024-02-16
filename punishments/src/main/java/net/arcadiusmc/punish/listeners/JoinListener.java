package net.arcadiusmc.punish.listeners;

import java.util.List;
import net.arcadiusmc.punish.GPermissions;
import net.arcadiusmc.punish.Note;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishPrefs;
import net.arcadiusmc.punish.Punishments;
import net.arcadiusmc.text.DefaultTextWriter;
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

    List<Note> notes = entry.getNotes();

    if (notes.isEmpty()) {
      return;
    }

    DefaultTextWriter writer = TextWriters.newWriter();
    writer.viewer(user);

    Note.writeNotes(notes, writer, user);

    ChannelledMessage ch = ChannelledMessage.create(writer.asComponent());
    ch.setBroadcast();
    ch.filterTargets(audience -> {
      User viewer = Audiences.getUser(audience);

      if (viewer == null) {
        return false;
      }

      if (!viewer.hasPermission(GPermissions.STAFF_NOTES)) {
        return false;
      }

      return viewer.get(PunishPrefs.VIEWS_NOTES);
    });
    ch.send();
  }
}
