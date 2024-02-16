package net.arcadiusmc.punish;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;

public interface GExceptions {
  static CommandSyntaxException invalidJailSpawn(Audience viewer) {
    return Messages.render("jails.invalidSpawn").exception(viewer);
  }

  static CommandSyntaxException cannotPunish(Audience viewer, User user) {
    return Messages.render("punishments.errors.cannotPunish")
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException alreadyPunished(Audience viewer, User user, PunishType type) {
    return Messages.render("punishments.errors.alreadyPunished")
        .addValue("player", user)
        .addValue("punished", type.namedEndingEd())
        .exception(viewer);
  }

  static CommandSyntaxException noNotes(Audience viewer, User user) {
    return Messages.render("staffNotes.noNotes")
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException cannotPardon(Audience viewer, User user, PunishType type) {
    return Messages.render("punishments.errors.cannotPardon")
        .addValue("player", user)
        .addValue("punishment", type.presentableName())
        .exception(viewer);
  }
}
