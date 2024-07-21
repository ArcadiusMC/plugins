package net.arcadiusmc.ui.commands;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.SuggestionFunction;
import net.arcadiusmc.ui.commands.PathParser.Parser;
import net.arcadiusmc.ui.resource.PageLoader;
import net.arcadiusmc.ui.resource.PagePath;
import net.arcadiusmc.ui.resource.PageRef;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.Suggester;

public class ModulePathParser implements ArgumentType<PageRef> {

  private final PageLoader manager;

  public ModulePathParser(PageLoader manager) {
    this.manager = manager;
  }

  @Override
  public PageRef parse(StringReader reader) throws CommandSyntaxException {
    RefParser<?> parser = new RefParser<>(reader);
    parser.parse();
    return parser.ref();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    RefParser<S> parser = new RefParser<>(Readers.forSuggestions(builder));

    try {
      parser.parse();
    } catch (CommandSyntaxException e) {
      // Ignored
    }

    return parser.getSuggestions(context, builder);
  }

  class RefParser<S> implements Suggester<S> {

    private final StringReader reader;

    String moduleName;

    Path modulePath;
    PagePath pagePath;

    private Suggester<S> suggester;

    public RefParser(StringReader reader) {
      this.reader = reader;
    }

    void suggest(int cursor, SuggestionFunction... func) {
      suggester = (context, builder) -> {
        if (cursor != builder.getStart()) {
          builder = builder.createOffset(cursor);
        }

        CommandSource source = (CommandSource) context.getSource();

        for (SuggestionFunction suggestionFunction : func) {
          suggestionFunction.suggest(builder, source);
        }

        return builder.buildFuture();
      };
    }

    void suggestPath(int cursor) {
      suggester = (context, builder) -> {
        builder = builder.createOffset(cursor);
        Parser<S> parser = new Parser<>(Readers.forSuggestions(builder), modulePath);

        try {
          parser.parse();
        } catch (CommandSyntaxException exc) {
          // ignored
        }

        return parser.getSuggestions(context, builder);
      };
    }

    void initialSuggestions(int cursor) {
      suggest(cursor, (builder, source) -> {
        Completions.suggest(builder, manager.getModuleNames());
      });
    }

    CommandSyntaxException invalidPath(int c) {
      reader.setCursor(c);
      return Exceptions.formatWithContext("Invalid module '{0}'", reader, moduleName);
    }

    void parse() throws CommandSyntaxException {
      int start = reader.getCursor();

      initialSuggestions(start);

      moduleName = reader.readUnquotedString();

      if (Strings.isNullOrEmpty(moduleName)) {
        throw invalidPath(start);
      } else {
        modulePath = manager.findModulePath(moduleName).getOrThrow(s -> invalidPath(start));
      }

      suggest(reader.getCursor(), (builder, source) -> {
        Completions.suggest(builder, ":");
      });

      if (!reader.canRead() || Character.isWhitespace(reader.peek())) {
        pagePath = new PagePath();
        return;
      }

      reader.expect(':');

      int pathStart = reader.getCursor();
      suggestPath(pathStart);

      Parser<S> pathParser = new Parser<>(reader, modulePath);
      pathParser.parse();

      pagePath = pathParser.createPath();
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
      if (suggester == null) {
        initialSuggestions(builder.getStart());
      }

      return suggester.getSuggestions(context, builder);
    }

    PageRef ref() {
      return new PageRef(moduleName, pagePath);
    }
  }
}
