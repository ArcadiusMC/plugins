package net.arcadiusmc.mail;

import com.google.gson.JsonArray;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.mail.Attachment.Builder;
import net.arcadiusmc.mail.event.MailReceiveEvent;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

class ServiceImpl implements MailService {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path path;

  @Getter
  private final MailMap map;

  private final MailPlugin plugin;

  public ServiceImpl(MailPlugin plugin) {
    this.plugin = plugin;
    this.path = PathUtil.pluginPath(plugin, "mail.json");
    this.map = new MailMap(this);
  }

  public void save() {
    SerializationHelper.writeFile(path, file -> {
      JsonArray arr = new JsonArray();
      map.save(arr);
      JsonUtils.writeFile(arr, file);
    });
  }

  public void load() {
    SerializationHelper.readFile(path,
        file -> JsonUtils.readFile(file).getAsJsonArray(),
        map::load
    );
  }

  @Override
  public Builder attachmentBuilder() {
    return new AttachmentImpl.BuilderImpl();
  }

  @Override
  public Mail.Builder mailBuilder() {
    return new MailBuilder();
  }

  @Override
  public void send(Mail apiType, MailSendFlag... flags) {
    sendQuietly(apiType);

    MailImpl mail = (MailImpl) apiType;
    User target = Users.get(apiType.getTarget());

    if (target.isOnline() && !ArrayUtils.contains(flags, MailSendFlag.NO_MESSAGE)) {
      Component mailDisplay = apiType.displayText(target, Page.EMPTY);

      String suffix = mail.isSenderVisible() && mail.getSender() != null
          ? ".player"
          : "";

      Component message = mail.mailMessage("mail.received" + suffix)
          .addValue("message", mailDisplay)
          .create(target);

      target.sendMessage(message);
    }

    if (plugin.getMailConfig().isDiscordForwardingAllowed()
        && PluginUtil.isEnabled("Arcadius-DiscordHook")
        && target.get(MailPrefs.MAIL_TO_DISCORD)
        && !ArrayUtils.contains(flags, MailSendFlag.NO_DISCORD)
        && !target.isOnline()
    ) {
      DiscordMail.forwardMailToDiscord((MailImpl) apiType, target);
    }
  }

  @Override
  public void sendQuietly(Mail apiType) {
    MailImpl mail = (MailImpl) apiType;
    map.add(mail);
    mail.sentDate = Instant.now();


    User target = Users.get(mail.getTarget());

    MailReceiveEvent event = new MailReceiveEvent(target, mail);
    event.callEvent();
  }

  @Override
  public Stream<? extends Mail> getMail(
      UUID targetId,
      @Nullable Instant cutoffDate,
      boolean keepDeleted
  ) {
    Objects.requireNonNull(targetId, "No playerId");

    var mailList = map.getByTarget(targetId);

    if (mailList == null || mailList.isEmpty()) {
      return Stream.empty();
    }

    Stream<MailImpl> stream = mailList.stream();

    if (cutoffDate != null) {
      stream = stream.filter(new CutOffFilter(cutoffDate));
    }

    if (!keepDeleted) {
      stream = stream.filter(DeletedFilter.INSTANCE);
    }

    return stream;
  }

  @Override
  public Stream<? extends Mail> query(boolean nonDeleted) {
    return map.getMailStream(nonDeleted);
  }

  @Override
  public boolean hasUnread(UUID playerId) {
    return map.hasUnread(playerId);
  }

  @Override
  public Mail getMessage(long messageId) {
    return map.getById(messageId);
  }

  enum DeletedFilter implements Predicate<Mail> {
    INSTANCE;

    @Override
    public boolean test(Mail mail) {
      return !mail.isDeleted();
    }
  }

  record CutOffFilter(Instant date) implements Predicate<Mail> {

    @Override
    public boolean test(Mail mail) {
      if (mail.getSentDate() == null || !mail.canBeOmitted()) {
        return true;
      }

      return !mail.getSentDate().isBefore(date);
    }
  }
}
