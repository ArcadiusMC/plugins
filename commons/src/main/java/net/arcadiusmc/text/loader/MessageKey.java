package net.arcadiusmc.text.loader;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

record MessageKey(String key, Map<String, String> arguments) {

  static final char ARGUMENTS_START = '(';
  static final char ARGUMENTS_END = ')';
  static final char KEY_VALUE_SEPARATOR = '=';

  static MessageKey parse(String key) throws CommandSyntaxException {
    return parse(new StringReader(key));
  }

  static MessageKey parse(StringReader reader) throws CommandSyntaxException {
    //
    // Grammar
    //
    // messageKey
    //   : key
    //   | key argumentList
    //   ;
    //
    // key
    //   : not('(')
    //   ;
    //
    // argumentList
    //   : '(' ')'
    //   : '('
    //   | '(' argument+ ')'
    //   | '(' argument+
    //   ;
    //
    // argument
    //   : identifier
    //   | '='
    //   | '=' identifier
    //   | identifier '='
    //   | identifier '=' identifier
    //   ;
    //

    StringBuilder key = new StringBuilder();
    Map<String, String> arguments = new Object2ObjectOpenHashMap<>();

    while (true) {
      if (!reader.canRead()) {
        return newKey(key, arguments);
      }

      if (reader.peek() == ARGUMENTS_START) {
        break;
      }
      if (Character.isWhitespace(reader.peek())) {
        reader.skipWhitespace();
        break;
      }

      key.append(reader.read());
    }

    reader.expect(ARGUMENTS_START);

    while (true) {
      reader.skipWhitespace();

      if (!reader.canRead()) {
        return newKey(key, arguments);
      }
      if (reader.peek() == ARGUMENTS_END) {
        reader.skip();
        break;
      }

      String argumentKey = readId(reader);
      reader.skipWhitespace();

      if (!reader.canRead() || reader.peek() != KEY_VALUE_SEPARATOR) {
        arguments.put(argumentKey, "");
        continue;
      }

      reader.expect(KEY_VALUE_SEPARATOR);
      reader.skipWhitespace();

      String argumentValue = readId(reader);
      arguments.put(argumentKey, argumentValue);
    }

    return newKey(key, arguments);
  }

  private static String readId(StringReader reader) {
    if (!reader.canRead()) {
      return "";
    }

    int start = reader.getCursor();

    while (reader.canRead() && isIdChar(reader.peek())) {
      reader.skip();
    }

    return reader.getString().substring(start, reader.getCursor());
  }

  private static boolean isIdChar(int ch) {
    return !Character.isWhitespace(ch)
        && ch != KEY_VALUE_SEPARATOR
        && ch != ARGUMENTS_END;
  }

  private static MessageKey newKey(StringBuilder builder, Map<String, String> args) {
    return new MessageKey(builder.toString(), args);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(key);

    if (!arguments.isEmpty()) {
      Iterator<Entry<String, String>> it = arguments.entrySet().iterator();

      builder.append('(');

      while (it.hasNext()) {
        Entry<String, String> e = it.next();

        builder.append(e.getKey());
        if (!Strings.isNullOrEmpty(e.getValue())) {
          builder.append("=").append(e.getValue());
        }

        if (it.hasNext()) {
          builder.append(' ');
        }
      }

      builder.append(')');
    }

    return builder.toString();
  }
}
