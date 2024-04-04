package net.arcadiusmc.core.commands.docs;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.core.commands.help.HelpArgument;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.io.PathUtil;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.FlagOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;

public class CommandDocGen extends BaseCommand {

  private static final FlagOption REMOVE_SQUARE_BRACKETS = Options.flag("remove-square-brackets");
  private static final FlagOption GEN_HEADER = Options.flag("add-wiki-header");
  private static final FlagOption GEN_ID_TAGS = Options.flag("add-id-html-tags");
  private static final FlagOption GEN_TOC = Options.flag("add-table-of-contents");

  private static final ArgumentOption<OutputType> TYPE
      = Options.argument(ArgumentTypes.enumType(OutputType.class), "type");

  private static final ArgumentOption<String> OUTPUT_FILE
      = Options.argument(StringArgumentType.string())
      .setLabel("output-file")
      .setDefaultValue("singleton.md")
      .setSuggester((context, builder) -> {
        return Completions.suggest(builder, "'singleton.md'", "'_index.md'");
      })
      .build();

  private static final ArgumentOption<List<String>> EXCLUDED
      = Options.argument(ArgumentTypes.array(new HelpArgument()))
      .setLabel("excluded")
      .setDefaultValue(List.of())
      .build();

  private static final ArgumentOption<List<String>> INCLUDED
      = Options.argument(ArgumentTypes.array(new HelpArgument()))
      .setLabel("included")
      .setDefaultValue(List.of())
      .build();

  private static final ArgumentOption<List<Group>> PLAYER_GROUPS
      = Options.argument(ArgumentTypes.array(GroupParser.TYPE))
      .setLabel("player-perm-groups")
      .setDefaultValue(List.of())
      .build();

  private static final OptionsArgument OPTIONS = OptionsArgument.builder()
      .addFlag(GEN_HEADER)
      .addFlag(REMOVE_SQUARE_BRACKETS)
      .addFlag(GEN_ID_TAGS)
      .addFlag(GEN_TOC)
      .addRequired(TYPE)
      .addOptional(OUTPUT_FILE)
      .addOptional(EXCLUDED)
      .addOptional(INCLUDED)
      .addOptional(PLAYER_GROUPS)
      .build();

  public CommandDocGen() {
    super("DocGen");

    setDescription("Generates documentation of all ArcadiusMC commands");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("options", OPTIONS)
            .executes(c -> {
              var opts = c.getArgument("options", ParsedOptions.class);
              opts.checkAccess(c.getSource());

              genDocs(c, opts);
              return 0;
            })
        );
  }

  private void genDocs(CommandContext<CommandSource> context, ParsedOptions options) {
    OutputType outType    = options.getValue(TYPE);
    String outFileName    = options.getValue(OUTPUT_FILE);

    assert outFileName != null;

    CommandDocs docs = new CommandDocs();
    docs.setRemoveSquareBrackets(options.has(REMOVE_SQUARE_BRACKETS));
    docs.setGenWikiHeader(options.has(GEN_HEADER));
    docs.setGenIdTags(options.has(GEN_ID_TAGS));
    docs.setGenContentTable(options.has(GEN_TOC));
    docs.setExcluded(options.getValue(EXCLUDED));
    docs.setIncluded(options.getValue(INCLUDED));
    docs.setPlayerGroups(options.getValue(PLAYER_GROUPS));

    docs.fill();

    Path pluginDir = PathUtil.pluginPath();
    Path docDir = pluginDir.resolve("cmd-docs");

    Path output = outType != OutputType.SEPARATED
        ? docDir.resolve(outFileName)
        : docDir;

    try {

      if (outType == OutputType.JSON) {
        docs.writeJson(output);
      } else if (outType == OutputType.SINGLE) {
        docs.writeSingleton(output);
      } else {
        docs.writeSeparated(output);
      }
    } catch (IOException exc) {
      throw new RuntimeException(exc);
    }

    context.getSource().sendSuccess(
        Text.format("Generated command documentation in folder '&f{0}&r'",
            NamedTextColor.GRAY,
            docDir
        )
    );
  }

  public enum OutputType {
    SINGLE,
    SEPARATED,
    JSON
  }

  private enum GroupParser implements ArgumentType<Group> {
    TYPE;

    @Override
    public Group parse(StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      String read = reader.readString();

      Group group = LuckPermsProvider.get().getGroupManager().getGroup(read);

      if (group == null) {
        reader.setCursor(start);
        throw Exceptions.formatWithContext("Unknown group: '{0}'", reader, read);
      }

      return group;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
      return Completions.suggest(builder,
          LuckPermsProvider.get().getGroupManager()
              .getLoadedGroups()
              .stream()
              .map(Group::getName)
      );
    }
  }
}