package net.arcadiusmc.holograms;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.parse.ChatParser;
import net.arcadiusmc.text.parse.TextContext;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.jetbrains.annotations.Nullable;

public class TextImpl extends Hologram implements TextHologram {

  static final TextReplacementConfig NEWLINES = TextReplacementConfig.builder()
      .matchLiteral("\\n")
      .replacement("\n")
      .build();

  @Getter @Setter
  private String text;

  public TextImpl(String name) {
    super(HOLOGRAM_KEY, name);
  }

  @Override
  public Component renderText(@Nullable Audience viewer) {
    if (Strings.isNullOrEmpty(text)) {
      return Component.empty();
    }

    TextContext context = TextContext.of(TEXT_FLAGS, viewer);
    Component component = ChatParser.parser().parse(text, context).replaceText(NEWLINES);

    return Placeholders.render(component, viewer);
  }

  public void copyFrom(TextImpl source) {
    this.text = source.text;
    displayMeta.copyFrom(source.displayMeta);
  }

  @Override
  protected void writeHover(TextWriter writer) {
    super.writeHover(writer);

    if (!Strings.isNullOrEmpty(text)) {
      writer.field("Text", editableTextFormat("/holograms content %s %s", text));
      writer.newLine();
      writer.newLine();
    }

    displayMeta.write(writer);
  }
}
