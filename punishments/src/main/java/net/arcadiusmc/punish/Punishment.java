package net.arcadiusmc.punish;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A single punishment instance given to a player.
 */
@Getter @Setter
@ToString
public class Punishment {

  public static final Codec<Punishment> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("source")
                .forGetter(Punishment::getSource),

            Codec.STRING.optionalFieldOf("reason", "")
                .forGetter(Punishment::getReason),

            Codec.STRING.optionalFieldOf("extra", "")
                .forGetter(Punishment::getExtra),

            PunishType.CODEC.fieldOf("type")
                .forGetter(Punishment::getType),

            ExtraCodecs.INSTANT.fieldOf("began")
                .forGetter(Punishment::getBegan),

            ExtraCodecs.INSTANT.optionalFieldOf("expires")
                .forGetter(o -> Optional.ofNullable(o.getExpires())),

            Codec.STRING.optionalFieldOf("pardoned_by", "")
                .forGetter(Punishment::getPardonSource),

            ExtraCodecs.INSTANT.optionalFieldOf("pardon_date")
                .forGetter(o -> Optional.ofNullable(o.getPardonDate()))
        )
        .apply(instance, (src, reason, extra, punishType, began, expires, pardoner, pardonDate) -> {
          Punishment punishment = new Punishment(
              punishType, src, reason, extra, began, expires.orElse(null)
          );

          punishment.setPardonSource(pardoner);
          pardonDate.ifPresent(punishment::setPardonDate);

          return punishment;
        });
  });

  /**
   * The name of staff member that created this punishment, in the case of a console-issued
   * punishment, this will be "Server"
   */
  private final String source;

  /**
   * The reason for the punishment, may be null
   */
  private final String reason;

  /**
   * Extra data stored in the punishment. Only used by {@link PunishType#JAIL} to store the name of
   * the jail cell the user was placed into.
   */
  private final String extra;

  /**
   * The punishment's type
   */
  private final PunishType type;

  /**
   * Timestamp of when the punishment was issued
   */
  private final Instant began;

  /**
   * Timestamp of when the punishment expire/when it did expire.
   * <p>
   * If a punishment wasn't given an expiry date, this will be {@code null}
   */
  @Nullable
  private final Instant expires;

  /**
   * If this punishment was lifted via a staff pardon, this will be the name of who pardoned it.
   * <p>
   * As was before, this will be "Server" if the pardon came from the console
   */
  private String pardonSource;

  /**
   * The date this punishment was pardoned, if it was pardoned. If this punishment has not been
   * pardoned, this will be <code>-1</code>
   */
  private Instant pardonDate;

  public Punishment(
      PunishType type,
      String source,
      @Nullable String reason,
      @Nullable String extra,
      @Nullable Instant began,
      @Nullable Instant expires
  ) {
    Objects.requireNonNull(source, "Null source");
    Objects.requireNonNull(type, "Null type");

    this.source = source;
    this.type = type;

    this.reason = Strings.nullToEmpty(reason);
    this.extra = Strings.nullToEmpty(extra);

    this.began = began == null ? Instant.now() : began;
    this.expires = expires;

    this.pardonSource = "";
  }

  public void setPardonSource(String pardonSource) {
    this.pardonSource = Strings.nullToEmpty(pardonSource);
  }

  /**
   * True, if this punishment was pardoned by a member of staff or the server console
   *
   * @return True, if this punishment instance was pardoned
   */
  public boolean wasPardoned() {
    return !Strings.isNullOrEmpty(pardonSource) && pardonDate != null;
  }

  /**
   * Writes info about this punishment into the given writer
   *
   * @param writer The writer to write to
   */
  public void writeDisplay(TextWriter writer) {
    writeField(writer, "Source", source);
    writeField(writer, "Began", Text.formatDate(began));
    writeField(writer, "Type", type.name().toLowerCase());

    if (expires != null) {
      writeField(writer, "Expires", Text.formatDate(expires));
    }

    writeField(writer, "Reason", reason);
    writeField(writer, "Extra", extra);

    if (wasPardoned()) {
      writeField(writer, "Pardon-date", Text.formatDate(pardonDate));
      writeField(writer, "Pardoned-by", pardonSource);
    }
  }

  private void writeField(TextWriter writer, String field, @Nullable String val) {
    if (Strings.isNullOrEmpty(val)) {
      return;
    }

    writer.field(field, val);
  }

  private void writeField(TextWriter writer, String field, @Nullable Component display) {
    if (display == null) {
      return;
    }

    writer.field(field, display);
  }
}