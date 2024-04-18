package net.arcadiusmc.dialogues;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
@Getter @Setter
public class DialogueOptions {

  static final Component DEFAULT_AVAILABLE = Text.renderString(" &b&l> &b${buttonText}");
  static final Component DEFAULT_UNAVAILABLE = Text.renderString(" &8&l> &7${buttonText}");
  static final Component DEFAULT_HIGHLIGHT = Text.renderString(" &6&l> &e${buttonText}");

  static final Codec<DialogueOptions> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.COMPONENT.optionalFieldOf("available-format", DEFAULT_AVAILABLE)
                .forGetter(DialogueOptions::getAvailableFormat),

            ExtraCodecs.COMPONENT.optionalFieldOf("unavailable-format", DEFAULT_UNAVAILABLE)
                .forGetter(DialogueOptions::getUnavailableFormat),

            ExtraCodecs.COMPONENT.optionalFieldOf("highlight-format", DEFAULT_HIGHLIGHT)
                .forGetter(DialogueOptions::getHighlightFormat),

            ExtraCodecs.COMPONENT.optionalFieldOf("prefix")
                .forGetter(o -> Optional.ofNullable(o.getPrefix())),

            ExtraCodecs.COMPONENT.optionalFieldOf("suffix")
                .forGetter(o -> Optional.ofNullable(o.getSuffix())),

            ExtraCodecs.KEY_CODEC.optionalFieldOf("entry-node", "")
                .forGetter(DialogueOptions::getEntryNode)
        )
        .apply(instance, (available, unavailable, highlight, prefix, suffix, entryPoint) -> {
          return new DialogueOptions(
              available,
              unavailable,
              highlight,
              prefix.orElse(null),
              suffix.orElse(null),
              entryPoint
          );
        });
  });

  private Component availableFormat;
  private Component unavailableFormat;
  private Component highlightFormat;
  private Component prefix;
  private Component suffix;
  private String entryNode;

  public static DialogueOptions defaultOptions() {
    return new DialogueOptions(
        DEFAULT_AVAILABLE,
        DEFAULT_UNAVAILABLE,
        DEFAULT_HIGHLIGHT,
        null,
        null,
        ""
    );
  }

  public void mergeFrom(DialogueOptions options) {
    if (Objects.equals(availableFormat, DEFAULT_AVAILABLE) || availableFormat == null) {
      this.availableFormat = options.availableFormat;
    }
    if (Objects.equals(unavailableFormat, DEFAULT_UNAVAILABLE) || unavailableFormat == null) {
      this.unavailableFormat = options.unavailableFormat;
    }
    if (Objects.equals(highlightFormat, DEFAULT_HIGHLIGHT) || highlightFormat == null) {
      this.highlightFormat = options.highlightFormat;
    }
    if (this.prefix == null) {
      this.prefix = options.prefix;
    }
    if (this.suffix == null) {
      this.suffix = options.suffix;
    }
  }
}