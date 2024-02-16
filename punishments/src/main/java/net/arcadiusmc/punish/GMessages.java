package net.arcadiusmc.punish;

import net.arcadiusmc.text.Messages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public interface GMessages {

  static Component noteMetadata(Note note, Audience viewer) {
    return Messages.render("staffNotes.meta")
        .addValue("writer", note.sourceName())
        .addValue("date", note.date())
        .create(viewer);
  }

  static Component noPunishmentPermission(Audience viewer, PunishType type) {
    return Messages.render("punishments.errors.noPermission")
        .addValue("punishment", type.presentableName())
        .create(viewer);
  }
}
