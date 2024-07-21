package net.arcadiusmc.ui.style;

import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.function.Function;
import net.arcadiusmc.ui.util.ParserErrors;
import net.arcadiusmc.ui.util.ParserErrors.Error;
import net.arcadiusmc.ui.util.ParserException;

public interface Styles {

  static DataResult<StylePropertyMap> parseInlineStyle(String source) {
    return parseStyle(source, StyleParser::inlineRules);
  }

  static DataResult<Stylesheet> parseStylesheet(String source) {
    return parseStyle(source, StyleParser::stylesheet);
  }

  private static <T> DataResult<T> parseStyle(String source, Function<StyleParser, T> call) {
    StyleParser parser = new StyleParser(new StringBuffer(source));
    T value;

    try {
      value = call.apply(parser);
    } catch (ParserException ignored) {
      value = null;
      // ignored, errors are handled up ahead
    }

    if (!parser.getErrors().isErrorPresent()) {
      return DataResult.success(value);
    }

    List<ParserErrors.Error> errors = parser.getErrors().getErrors();
    StringBuilder builder = new StringBuilder();

    builder.append("Failed to parse style: ");

    for (Error error : errors) {
      builder.append("\n\n[")
          .append(error.level().toString())
          .append("] ");

      builder.append(error.message());
    }

    return DataResult.error(builder::toString);
  }
}
