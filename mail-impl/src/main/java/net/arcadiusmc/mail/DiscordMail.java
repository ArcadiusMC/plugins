package net.arcadiusmc.mail;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.discord.DiscordHook;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

class DiscordMail {
  private static final Logger LOGGER = Loggers.getLogger();

  static void forwardMailToDiscord(MailImpl mail, User target) {
    Optional<Member> opt = DiscordHook.getUserMember(target);

    if (opt.isEmpty()) {
      return;
    }

    Member member = opt.get();

    member.getUser().openPrivateChannel()
        .submit()
        .whenComplete((privateChannel, openingError) -> {
          if (openingError != null) {
            LOGGER.error("Couldn't open private channel for {} (member.tag={})",
                target.getName(),
                member.getUser().getName(),
                openingError
            );

            return;
          }

          StringBuilder builder = new StringBuilder();
          builder.append("You've got mail!");

          if (mail.isSenderVisible() && mail.getSender() != null) {
            User user = Users.get(mail.getSender());
            String userName = DiscordHook.getUserMember(user)
                .map(m -> user.getName() + " (Discord ID: " + m.getUser().getName() + ")")
                .orElse(user.getName());

            builder.append(" From ");
            builder.append(userName);
            builder.append(".");
          }

          AttachmentState attachState = mail.getAttachmentState();
          if (attachState == AttachmentState.UNCLAIMED) {
            builder.append(" Message has items!");
          }

          //
          // Rendering the text in ANSI is the most amount of effort I can be bothered to give. I
          // tried rendering text to a PNG but there was a thousand issues with that. And the text
          // wouldn't show up in the discord notification anyway, so ANSI is the best I can do to
          // both support colors and let you see the message in the discord notification
          //
          // If we wanted to do PNG route, we'd need to reverse engineer the minecraft renderer
          // because jesus christ, minecraft has 3 fonts and each is rendered differently and a
          // single font, rendered by AWT, can't replicate how Minecraft does it
          //

          builder.append("\n```ansi\n");

          Component text = mail.getMessage().create(target);
          AnsiText ansi = new AnsiText(builder,
              Message.MAX_CONTENT_LENGTH
                  - "\n```".length()
                  - AnsiText.SUFFIX.length()
          );
          Text.FLATTENER.flatten(text, ansi);

          builder.append("\n```");
          builder.append(AnsiText.SUFFIX);

          String fullString = builder.toString();
          LOGGER.debug("Rendered discord message:\n{}", AnsiText.replaceControlCodes(fullString));

          privateChannel.sendMessage(fullString).submit().whenComplete((message, sendingError) -> {
            if (sendingError != null) {
              LOGGER.error("Error sending discord mail forward!", sendingError);
              return;
            }

            LOGGER.debug("Forwarded in-game mail to {}'s discord (member.tag={})",
                target.getName(),
                member.getUser().getName()
            );
          });
        });
  }
}
