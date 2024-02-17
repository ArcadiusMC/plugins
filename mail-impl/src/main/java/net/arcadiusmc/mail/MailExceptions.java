package net.arcadiusmc.mail;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Instant;
import net.arcadiusmc.text.Messages;
import net.kyori.adventure.audience.Audience;

public interface MailExceptions {

  static CommandSyntaxException claimNotAllowed(Audience viewer) {
    return Messages.render("mail.errors.claimForbidden").exception(viewer);
  }

  static CommandSyntaxException alreadyClaimed(Audience viewer) {
    return Messages.render("mail.errors.alreadyClaimed").exception(viewer);
  }

  static CommandSyntaxException attachmentExpired(Audience viewer, Instant expirationDate) {
    if (expirationDate == null) {
      return Messages.render("mail.errors.expired.dateMissing").exception(viewer);
    }

    return Messages.render("mail.errors.expired")
        .addValue("date", expirationDate)
        .exception(viewer);
  }
}
