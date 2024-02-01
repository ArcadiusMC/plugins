package net.arcadiusmc.text.loader;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.utils.io.FtcCodecs;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;

public enum StyleStringCodec implements PrimitiveCodec<Style> {
  CODEC;

  @Override
  public <T> DataResult<Style> read(DynamicOps<T> ops, T input) {
    return ops.getStringValue(input).flatMap(string -> {
      return FtcCodecs.safeParse(string, reader -> {
        var builder = Style.style();
        parseStyle(reader, builder);
        return builder.build();
      });
    });
  }

  @Override
  public <T> T write(DynamicOps<T> ops, Style value) {
    StringBuilder builder = new StringBuilder();

    if (value.color() != null) {
      builder.append(value.color());
    }

    value.decorations().forEach((decoration, state) -> {
      if (state == State.NOT_SET) {
        return;
      }

      builder.append(';');

      if (state != State.TRUE) {
        builder.append('!');
      }

      builder.append(decoration.toString().toLowerCase());
    });

    return ops.createString(builder.toString());
  }


  public static void parseStyle(StringReader reader, Style.Builder builder)
      throws CommandSyntaxException
  {
    reader.skipWhitespace();

    // If the input is "" then it's just an empty style
    if (!reader.canRead()) {
      return;
    }

    // ";bold;italic", color not specified, but should still be valid
    if (reader.peek() != ';') {
      builder.color(Arguments.COLOR.parse(reader));
    }

    reader.skipWhitespace();

    while (reader.canRead() && reader.peek() == ';') {
      reader.skip();
      reader.skipWhitespace();

      if (!reader.canRead()) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
            .readerExpectedSymbol()
            .createWithContext(reader, "decoration name or !");
      }

      boolean state;

      if (reader.peek() == '!') {
        reader.skip();
        state = false;
      } else {
        state = true;
      }

      String label = reader.readUnquotedString();

      switch (label.toLowerCase()) {
        case "italic", "o"        -> builder.decoration(TextDecoration.ITALIC, state);
        case "bold", "l"          -> builder.decoration(TextDecoration.BOLD, state);
        case "strikethrough", "m" -> builder.decoration(TextDecoration.STRIKETHROUGH, state);
        case "underlined"         -> builder.decoration(TextDecoration.UNDERLINED, state);
        case "obfuscated", "k"    -> builder.decoration(TextDecoration.OBFUSCATED, state);

        default -> throw Exceptions.format("Unknown decoration: '{0}'", label);
      }

      reader.skipWhitespace();
    }
  }
}
