package net.arcadiusmc.core.tab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.utils.io.FtcCodecs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

record TabText(Component header, Component footer) {
  public static final TabText EMPTY = new TabText(Component.empty(), Component.empty());

  static final Codec<TabText> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            FtcCodecs.COMPONENT
                .optionalFieldOf("header", Component.empty())
                .forGetter(o -> o.header),

            FtcCodecs.COMPONENT
                .optionalFieldOf("footer", Component.empty())
                .forGetter(o -> o.footer)
        )

        .apply(instance, TabText::of);
  });

  static TabText of(Component header, Component footer) {
    if (Text.isEmpty(header) && Text.isEmpty(footer)) {
      return EMPTY;
    }
    return new TabText(header, footer);
  }

  TabText combine(TabText other) {
    return of(
        Component.textOfChildren(header, other.header),
        Component.textOfChildren(footer, other.footer)
    );
  }

  TabText apply(PlaceholderRenderer renderer, @Nullable Audience viewer) {
    return of(renderer.render(header, viewer), renderer.render(footer, viewer));
  }

  TabText append(TabText other) {
    return of(header.append(other.header), footer.append(other.footer));
  }
}
