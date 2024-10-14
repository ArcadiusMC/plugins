package net.arcadiusmc.core.tab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.text.loader.StyleStringCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
class TabConfig {

  static final Codec<TabConfig> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            TabText.CODEC
                .optionalFieldOf("base", TabText.EMPTY)
                .forGetter(o -> o.base),

            TabText.CODEC
                .optionalFieldOf("score-append", TabText.EMPTY)
                .forGetter(o -> o.scoreAppend),

            TabText.CODEC
                .optionalFieldOf("score-prepend", TabText.EMPTY)
                .forGetter(o -> o.scorePrepend),

            DynamicBorderConfig.CODEC
                .optionalFieldOf("adaptive-border", DynamicBorderConfig.DEFAULT)
                .forGetter(o -> o.borderConfig),

            ExtraCodecs.DURATION
                .optionalFieldOf("frame-rate", Duration.ZERO)
                .forGetter(o -> o.animationSpeed),

            Codec.BOOL
                .optionalFieldOf("reverse-animation-at-end", false)
                .forGetter(o -> o.reverseAnimationAtEnd),

            TabText.CODEC.listOf()
                .optionalFieldOf("animation-frames", List.of())
                .forGetter(o -> o.frames)
        )

        .apply(instance, TabConfig::new);
  });

  private TabText base = TabText.EMPTY;

  private TabText scoreAppend = TabText.EMPTY;
  private TabText scorePrepend = TabText.EMPTY;

  private DynamicBorderConfig borderConfig = DynamicBorderConfig.DEFAULT;

  private Duration animationSpeed = Duration.ZERO;
  private boolean reverseAnimationAtEnd = false;
  private List<TabText> frames = List.of();

  static TabConfig defaultConfig() {
    return new TabConfig();
  }

  boolean isAnimated() {
    return animationSpeed != null && animationSpeed.isPositive();
  }

  record DynamicBorderConfig(
      int connectionBarWidth,
      int headWidth,
      int minWidth,
      int heartsWidth,
      int overReach,
      String borderChar,
      Style style,
      TextColor[] gradientColors
  ) {
    static final Style DEFAULT_STYLE = Style.style(TextDecoration.UNDERLINED);

    static final DynamicBorderConfig DEFAULT
        = new DynamicBorderConfig(10, 8, 96, 81, 0, " ", DEFAULT_STYLE, new TextColor[0]);

    static final Codec<DynamicBorderConfig> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.INT.optionalFieldOf("connection-bar-width-px", DEFAULT.connectionBarWidth)
                  .forGetter(o -> o.connectionBarWidth),

              Codec.INT.optionalFieldOf("head-width-px", DEFAULT.headWidth)
                  .forGetter(o -> o.headWidth),

              Codec.INT.optionalFieldOf("min-width-px", DEFAULT.minWidth)
                  .forGetter(o -> o.minWidth),

              Codec.INT.optionalFieldOf("hearts-width-px", DEFAULT.headWidth)
                  .forGetter(o -> o.heartsWidth),

              Codec.INT.optionalFieldOf("over-reach-px", DEFAULT.headWidth)
                  .forGetter(o -> o.overReach),

              Codec.STRING.optionalFieldOf("char", DEFAULT.borderChar)
                  .forGetter(o -> o.borderChar),

              StyleStringCodec.CODEC.optionalFieldOf("style", DEFAULT_STYLE)
                  .forGetter(o -> o.style),

              ExtraCodecs.COLOR.listOf().optionalFieldOf("gradient", List.of())
                  .xmap(textColors -> textColors.toArray(TextColor[]::new), Arrays::asList)
                  .forGetter(o -> o.gradientColors)
          )
          .apply(instance, DynamicBorderConfig::new);
    });

    int extraPixels() {
      return connectionBarWidth + headWidth;
    }

    String create(int widthPx) {
      int chWidth = TextInfo.getPxWidth(borderChar);
      int charCount = Math.max(1, widthPx / chWidth);
      return borderChar.repeat(charCount);
    }

    Component createText(int widthPx) {
      String repeated = create(widthPx);

      if (gradientColors.length > 0) {
        return Text.gradient(repeated, true, gradientColors).style(style);
      }

      return Component.text(repeated, style);
    }
  }
}
