package net.arcadiusmc.mail.command;

import java.util.Collection;
import lombok.Getter;
import net.arcadiusmc.mail.Mail;
import net.arcadiusmc.mail.Mail.Builder;

@Getter
public enum MailSendOption {
  ANONYMOUS {
    @Override
    void apply(Builder builder) {
      builder.hideSender(true);
    }
  };

  abstract void apply(Mail.Builder builder);

  static void apply(Collection<MailSendOption> options, Mail.Builder builder) {
    if (options == null || options.isEmpty()) {
      return;
    }

    for (MailSendOption option : options) {
      option.apply(builder);
    }
  }
}
