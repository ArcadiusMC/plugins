package net.arcadiusmc.core.placeholder;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.ParsedPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ArrayArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

/**
 * <pre>
 * ${name}
 * ${name:number}
 * ${name:number;color1[',' color]+}
 * </pre>
 */
public class RepeatedCharPlaceholder implements ParsedPlaceholder {

  static final ArrayArgument<TextColor> COLOR_ARRAY = ArgumentTypes.array(Arguments.COLOR);
  static final int DEFAULT_LENGTH = 5;

  private final boolean strikethrough;

  public RepeatedCharPlaceholder(boolean strikethrough) {
    this.strikethrough = strikethrough;
  }

  @Override
  public @Nullable Component render(StringReader reader, PlaceholderContext context)
      throws CommandSyntaxException
  {
    int chars = readLength(reader);
    reader.skipWhitespace();

    if (reader.canRead() && reader.peek() == ';') {
      reader.skip();
      reader.skipWhitespace();
    }

    List<TextColor> gradientColors = readGradient(reader);

    String str = " ".repeat(chars);

    if (gradientColors.isEmpty()) {
      return strikethrough
          ? Component.text(str).decorate(TextDecoration.STRIKETHROUGH)
          : Component.text(str);
    }

    Component gradiented = Text.gradient(str, true, gradientColors);

    return strikethrough
        ? gradiented.decorate(TextDecoration.STRIKETHROUGH)
        : gradiented;
  }

  List<TextColor> readGradient(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      return List.of();
    }
    return COLOR_ARRAY.parse(reader);
  }

  int readLength(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      return DEFAULT_LENGTH;
    }

    char peeked = reader.peek();

    if (peeked >= '0' && peeked <= '9') {
      return Readers.readPositiveInt(reader, 1, Integer.MAX_VALUE);
    }

    return DEFAULT_LENGTH;
  }
}
