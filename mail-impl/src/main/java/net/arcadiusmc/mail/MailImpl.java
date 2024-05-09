package net.arcadiusmc.mail;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.mail.command.MailCommands;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.ViewerAwareMessage.WrappedComponent;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.text.page.Footer;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class MailImpl implements Mail {

  private static final String KEY_SENDER = "sender";
  private static final String KEY_TARGET = "target";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_ATTACHMENT = "attachment";
  private static final String KEY_CLAIM_DATE = "claim_date";
  private static final String KEY_SENT_DATE = "sent_date";
  private static final String KEY_READ_DATE = "read_date";
  private static final String KEY_ID = "message_id";
  private static final String KEY_HIDE_SENDER = "hide_sender";
  private static final String KEY_ATTACHMENT_EXPIRY = "attachment_expire_date";
  private static final String KEY_DELETED = "deleted";

  @Include
  private final UUID sender;

  @Include
  private final UUID target;

  @Include
  private final ViewerAwareMessage message;

  @Include
  private final AttachmentImpl attachment;

  @Setter
  private Instant claimDate;
  @Setter
  private Instant attachmentExpiry;

  ServiceImpl service;

  Instant sentDate;
  private Instant readDate;

  private boolean deleted;
  private boolean hideSender;

  @Include
  long mailId = NULL_ID;

  public MailImpl(MailBuilder builder) {
    this.sender           = builder.sender;
    this.target           = builder.target;
    this.message          = builder.message;
    this.attachment       = builder.attachment;
    this.hideSender       = builder.hideSender;
    this.attachmentExpiry = builder.attachmentExpiry;
  }
  
  /* --------------------------- SERIALIZATION ---------------------------- */
  
  public static Result<MailImpl> load(JsonElement element) {
    if (!element.isJsonObject()) {
      return Result.error("Not an object");
    }

    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());
    MailBuilder builder = new MailBuilder();
    
    if (!json.has(KEY_TARGET)) {
      return Result.error("No '%s' value", KEY_TARGET);
    } else {
      JsonElement targetElem = json.get(KEY_TARGET);
      var idResult = loadUuid(targetElem);
      
      if (idResult.isError()) {
        return idResult.map(s -> "Couldn't load 'target': " + s).cast();
      }
      
      builder.target = idResult.getValue();
    }
    
    if (json.has(KEY_SENDER)) {
      var idResult = loadUuid(json.get(KEY_SENDER));

      if (idResult.isError()) {
        return idResult.mapError(string -> "Couldn't load 'sender': " + string).cast();
      }

      builder.sender = idResult.getValue();
    }
    
    if (!json.has(KEY_MESSAGE)) {
      return Result.error("No '%s' value", KEY_MESSAGE);
    } else {
      var messageResult = loadMessage(json.get(KEY_MESSAGE));
      
      if (messageResult.isError()) {
        return messageResult.mapError(string -> "Couldn't load message: " + string).cast();
      }
      
      builder.message = messageResult.getValue();
    }

    if (json.has(KEY_ATTACHMENT)) {
      var attachmentResult = AttachmentImpl.load(json.get(KEY_ATTACHMENT));

      if (attachmentResult.isError()) {
        return attachmentResult.mapError(string -> "Couldn't load attachment: " + string).cast();
      }

      builder.attachment = attachmentResult.getValue();
    }

    MailImpl built = builder.build();

    built.mailId            = json.getLong(KEY_ID, NULL_ID);
    built.hideSender        = json.getBool(KEY_HIDE_SENDER, false);
    built.deleted           = json.getBool(KEY_DELETED, false);
    built.claimDate         = JsonUtils.readInstant(json.get(KEY_CLAIM_DATE));
    built.sentDate          = JsonUtils.readInstant(json.get(KEY_SENT_DATE));
    built.readDate          = JsonUtils.readInstant(json.get(KEY_READ_DATE));
    built.attachmentExpiry  = JsonUtils.readInstant(json.get(KEY_ATTACHMENT_EXPIRY));

    return Result.success(built);
  }
  
  private static Result<UUID> loadUuid(JsonElement element) {
    try {
      return Result.success(JsonUtils.readUUID(element));
    } catch (IllegalArgumentException exc) {
      return Result.error(exc.getMessage());
    }
  }
  
  private static Result<ViewerAwareMessage> loadMessage(JsonElement element) {
    if (!element.isJsonObject()) {
      return Result.error("Not an object");
    }
    
    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());

    if (json.has("message")) {
      DataResult<PlayerMessage> dataResult 
          = PlayerMessage.load(new Dynamic<>(JsonOps.INSTANCE, element));
      
      if (dataResult.error().isPresent()) {
        return Result.error(dataResult.error().get().message());
      }
      
      return Result.success(dataResult.result().get());
    }
    
    try {
      return Result.success(JsonUtils.readText(element)).map(ViewerAwareMessage::wrap);
    } catch (JsonSyntaxException exc) {
      return Result.error(exc.getMessage());
    }
  }
  
  public JsonElement save() {
    JsonWrapper json = JsonWrapper.create();
    json.addUUID(KEY_TARGET, target);
    json.add(KEY_MESSAGE, saveMessage());
    json.add(KEY_ID, mailId);

    if (sender != null) {
      json.addUUID(KEY_SENDER, sender);
    }

    if (deleted) {
      json.add(KEY_DELETED, true);
    }

    if (hasAttachment()) {
      json.add(KEY_ATTACHMENT, attachment.save());

      if (claimDate != null) {
        json.add(KEY_CLAIM_DATE, JsonUtils.writeInstant(claimDate));
      }

      if (attachmentExpiry != null) {
        json.add(KEY_ATTACHMENT_EXPIRY, JsonUtils.writeInstant(attachmentExpiry));
      }
    }

    if (sentDate != null) {
      json.add(KEY_SENT_DATE, JsonUtils.writeInstant(sentDate));
    }

    if (readDate != null) {
      json.add(KEY_READ_DATE, JsonUtils.writeInstant(readDate));
    }

    if (hideSender) {
      json.add(KEY_HIDE_SENDER, true);
    }

    return json.getSource();
  }

  private JsonElement saveMessage() {
    if (message instanceof PlayerMessage player) {
      return player.save(JsonOps.INSTANCE).getOrThrow();
    }

    WrappedComponent wrapped = (WrappedComponent) message;
    return JsonUtils.writeText(wrapped.text());
  }
  
  /* ---------------------------------------------------------------------- */

  @Override
  public AttachmentState getAttachmentState() {
    if (attachment == null || attachment.isEmpty()) {
      return AttachmentState.NO_ATTACHMENT;
    }

    if (claimDate != null) {
      return AttachmentState.CLAIMED;
    }

    if (attachmentExpiry == null) {
      return AttachmentState.UNCLAIMED;
    }

    Instant now = Instant.now();
    if (now.isAfter(attachmentExpiry)) {
      return AttachmentState.EXPIRED;
    }

    return AttachmentState.UNCLAIMED;
  }

  @Override
  public MessageType getMessageType() {
    return message instanceof PlayerMessage
        ? MessageType.PLAYER
        : MessageType.REGULAR;
  }

  MessageRender mailMessage(String key) {
    MessageRender render = Messages.render(key);

    if (sentDate != null) {
      render.addValue("sent_date", sentDate);
    }

    if (readDate != null) {
      render.addValue("read_date", readDate);
    }

    if (attachmentExpiry != null) {
      render.addValue("attach_expire", attachmentExpiry);
    }

    if (sender != null) {
      render.addValue("sender", Users.get(sender));
    }

    render.addValue("target", Users.get(target));

    return render;
  }

  @Override
  public Component displayText(Audience viewer, Page page) {
    TextJoiner firstLineJoiner = TextJoiner.onSpace();
    firstLineJoiner.add(readButton(viewer, page));

    if (hasAttachment()) {
      firstLineJoiner.add(claimButton(viewer, page));
    }

    firstLineJoiner.add(deleteButton(viewer, page));
    firstLineJoiner.add(infoButton(viewer));

    Component firstLine  = firstLineJoiner.asComponent();
    Component secondLine = formatMessage(viewer);

    return mailMessage("mail.display")
        .addValue("header", firstLine)
        .addValue("message", secondLine)
        .create(viewer);
  }

  @Override
  public Component deleteButton(Audience viewer, Page page) {
    if (deleted) {
      return mailMessage("mail.buttons.deleted").create(viewer)
          .hoverEvent(mailMessage("mail.buttons.deleted.hover").create(viewer));
    }

    return mailMessage("mail.buttons.delete").create(viewer)
        .hoverEvent(mailMessage("mail.buttons.delete.hover").create(viewer))
        .clickEvent(ClickEvent.runCommand(MailCommands.getDeleteCommand(this, page)));
  }

  @Override
  public Component infoButton(Audience viewer) {
    return mailMessage("mail.buttons.info").create(viewer)
        .hoverEvent(metadataText(viewer));
  }

  @Override
  public Component claimButton(Audience viewer, Page page) {
    if (!hasAttachment()) {
      return null;
    }

    AttachmentState attachState = getAttachmentState();
    AttachmentImpl attach = getAttachment();

    String textKey;
    String hoverKey;

    switch (attachState) {
      case CLAIMED -> {
        textKey = "mail.buttons.claim.taken";
        hoverKey = textKey + ".hover";
      }

      case UNCLAIMED -> {
        textKey = "mail.buttons.claim.regular";

        if (attach != null && attachmentExpiry != null) {
          hoverKey = "mail.buttons.claim.limited.hover";
        } else {
          hoverKey = textKey + ".hover";
        }
      }

      case EXPIRED -> {
        textKey = "mail.buttons.claim.expired";
        hoverKey = textKey + ".hover";
      }

      default -> {
        // Not possible
        throw new AssertionError();
      }
    }

    TextWriter writer = TextWriters.newWriter();
    writer.viewer(viewer);
    writer.line(mailMessage(hoverKey).create(viewer));

    attach.write(writer);

    return mailMessage(textKey).create(viewer)
        .hoverEvent(writer.asComponent())
        .clickEvent(ClickEvent.runCommand(MailCommands.getClaimCommand(this, page)));
  }

  @Override
  public Component readButton(Audience viewer, Page page) {
    boolean read = isRead();

    String messageKey = "mail.buttons." + (read ? "markUnread" : "markRead");

    Component base = mailMessage(messageKey).create(viewer);
    Component hover = mailMessage(messageKey + ".hover").create(viewer);

    return base
        .clickEvent(ClickEvent.runCommand(MailCommands.getReadToggleCommand(this, page)))
        .hoverEvent(hover);
  }

  @Override
  public Component formatMessage(Audience viewer) {
    Component formattedText = message.create(viewer);

    if (!isAdminMessage()) {
      return formattedText;
    }

    PlaceholderRenderer list = Placeholders.newRenderer();
    list.useDefaults();

    if (sentDate != null) {
      list.add("sent_date", () -> Text.formatDate(sentDate));
    }

    if (readDate != null) {
      list.add("read_date", () -> Text.formatDate(readDate));
    }

    if (attachmentExpiry != null) {
      list.add("attach_expire", () -> Text.formatDate(attachmentExpiry));
    }

    if (sender != null) {
      Placeholders.createPlayerPlaceholders(list, "sender", sender);
    }

    Placeholders.createPlayerPlaceholders(list, "target", target);

    return list.render(formattedText, viewer);
  }

  @Override
  public Component metadataText(Audience viewer) {
    var writer = TextWriters.newWriter();
    writer.viewer(viewer);

    writer.line("Mail Message");

    if (sentDate != null) {
      writer.line(mailMessage("mail.meta.sent"));
    }

    if (sender != null && isSenderVisible()) {
      writer.line(mailMessage("mail.meta.sentBy"));
    }

    if (hasAttachment()) {
      writer.line(Footer.GENERIC_BORDER);

      if (claimDate != null) {
        writer.line(mailMessage("mail.meta.claimDate"));
      }

      attachment.write(writer);
    }

    return writer.asComponent();
  }

  @Override
  public boolean canBeOmitted() {
    if (!isRead()) {
      return false;
    }

    if (!hasAttachment()) {
      return true;
    }

    return claimDate != null;
  }

  @Override
  public boolean isSenderVisible() {
    return !hideSender;
  }

  @Override
  public void delete() {
    deleted = true;

    if (service != null) {
      service.getMap().updateDeletedState(this);
    }
  }

  @Override
  public boolean toggleRead() {
    if (readDate != null) {
      readDate = null;
      return false;
    }

    readDate = Instant.now();
    return true;
  }

  @Override
  public void claimAttachment(Player player) throws CommandSyntaxException {
    var state = getAttachmentState();

    if (state == AttachmentState.NO_ATTACHMENT) {
      return;
    }

    if (state == AttachmentState.EXPIRED) {
      throw MailExceptions.attachmentExpired(player, attachmentExpiry);
    }

    if (state == AttachmentState.CLAIMED) {
      throw MailExceptions.alreadyClaimed(player);
    }

    attachment.claim(player);
    Component claimMessage = attachment.claimMessage(player);

    if (player.getUniqueId().equals(target)) {
      claimDate = Instant.now();
    }

    player.sendMessage(claimMessage);
  }
}

