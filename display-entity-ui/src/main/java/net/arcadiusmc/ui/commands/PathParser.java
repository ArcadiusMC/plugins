package net.arcadiusmc.ui.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.ui.resource.PagePath;
import net.arcadiusmc.utils.io.PathUtil;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.Suggester;

public class PathParser implements ArgumentType<PagePath> {

  @Override
  public PagePath parse(StringReader reader) throws CommandSyntaxException {
    Parser<?> parser = new Parser<>(reader, null);
    parser.parse();
    return parser.createPath();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    StringReader reader = Readers.forSuggestions(builder);
    Parser<S> parser = new Parser<>(reader, null);

    try {
      parser.parse();
    } catch (CommandSyntaxException exc) {
      // ignored
    }

    return parser.getSuggestions(context, builder);
  }

  static class Parser<S> implements Suggester<S> {

    final PagePath path = new PagePath();

    final StringReader reader;
    final Path moduleRoot;

    Path directory;

    public Parser(StringReader reader, Path moduleRoot) {
      this.reader = reader;
      this.moduleRoot = moduleRoot;
      this.directory = moduleRoot;
    }

    void parse() throws CommandSyntaxException {
      reader.skipWhitespace();

      while (true) {
        if (!reader.canRead()) {
          return;
        }

        String str = readPathElement();

        if (!str.isEmpty()) {
          path.addElement(str);
        }

        if (!reader.canRead()) {
          return;
        }

        char ch = reader.peek();

        if (Character.isWhitespace(ch)) {
          // end of argument
          return;
        }

        if (ch == '?') {
          break;
        }

        reader.expect('/');
        directory = moduleRoot.resolve(path.elements());
      }

      if (!reader.canRead()) {
        return;
      }
      if (reader.peek() != '?') {
        return;
      }

      reader.skip();
      while (reader.canRead()) {
        String key = reader.readUnquotedString();
        String value;

        if (reader.canRead()) {
          if (reader.peek() == '=') {
            reader.skip();
            value = reader.readUnquotedString();
          } else {
            value = "";
          }
        } else {
          value = "";
        }

        path.putQuery(key, value);

        if (reader.canRead()) {
          return;
        }

        if (Character.isWhitespace(reader.peek())) {
          return;
        }

        reader.expect('&');
      }
    }

    private String readPathElement() throws CommandSyntaxException {
      char peek = reader.peek();

      if (peek == '"' || peek == '\'') {
        reader.skip();
        return readQuotedElement(peek);
      }

      return readUnquotedElement();
    }

    private String readUnquotedElement() {
      int start = reader.getCursor();

      while (reader.canRead() && isValidFileChar(reader.peek())) {
        reader.skip();
      }

      return reader.getString().substring(start, reader.getCursor());
    }

    private String readQuotedElement(char quote) throws CommandSyntaxException {
      int start = reader.getCursor();

      while (true) {
        if (!reader.canRead()) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
              .readerExpectedEndOfQuote()
              .createWithContext(reader);
        }

        if (reader.peek() == quote) {
          reader.skip();
          break;
        }

        char ch = reader.peek();

        if (isValidFileChar(ch) || ch == ' ') {
          reader.skip();
          continue;
        }

        throw Exceptions.formatWithContext("Invalid character '{0}' in file path", reader, ch);
      }

      return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isValidFileChar(char ch) {
      return StringReader.isAllowedInUnquotedString(ch) || ch == '$' || ch == '%';
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
      if (moduleRoot == null) {
        return Suggestions.empty();
      }

      List<String> paths = PathUtil.findAllFiles(directory, true);
      return Completions.suggest(builder, paths);
    }

    public PagePath createPath() {
      return path;
    }
  }
}
