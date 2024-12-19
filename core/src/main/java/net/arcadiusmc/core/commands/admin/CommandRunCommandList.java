package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.utils.io.PathUtil;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.GrenadierCommand;
import org.slf4j.Logger;

public class CommandRunCommandList extends BaseCommand {

  private static final Logger LOGGER = Loggers.getLogger();

  private final CorePlugin plugin;
  private final Path directory;

  public CommandRunCommandList(CorePlugin plugin) {
    super("runlist");

    this.plugin = plugin;
    this.directory = plugin.getDataPath().resolve("command-lists");

    setAliases("run-cmd-list", "runcmdlist", "run-list");
    setDescription("Runs a .txt file of commands");

    register();

    try {
      PathUtil.ensureDirectoryExists(this.directory);
    } catch (RuntimeException exc) {
      LOGGER.warn("Error creating command lists directory", exc);
    }
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("file", StringArgumentType.greedyString())
            .suggests((c, b) -> {
              if (!Files.isDirectory(directory)) {
                return Suggestions.empty();
              }

              List<String> files = PathUtil.findAllFiles(
                  directory,
                  true,
                  path -> !Files.isDirectory(path)
              );

              return Completions.suggest(b, files);
            })

            .executes(c -> {
              String fname = c.getArgument("file", String.class);
              Path p = directory.resolve(fname);

              if (!Files.exists(p)) {
                throw Exceptions.format("File {0} doesn't exist, can run commands", p);
              }

              List<String> lines;

              try {
                lines = Files.readAllLines(p, StandardCharsets.UTF_8);
              } catch (IOException exc) {
                LOGGER.error("Failed to read command list file {}", p, exc);
                throw Exceptions.format("Error reading file {0}, check console", p);
              }

              for (int i = 0; i < lines.size(); i++) {
                String n = lines.get(i);

                if (Strings.isNullOrEmpty(n) || n.isBlank()) {
                  continue;
                }

                Grenadier.dispatch(c.getSource(), n);
              }

              return SINGLE_SUCCESS;
            })
        );
  }
}
