package net.arcadiusmc.core.commands.admin;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserFlags;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandUserFlags extends BaseCommand {

  private final CorePlugin plugin;

  public CommandUserFlags(CorePlugin plugin) {
    super("user-flags");

    this.plugin = plugin;

    setAliases("player-flags");
    setPermission(CorePermissions.CMD_USERFLAGS);
    setDescription("Modifies user flags");

    register();
  }

  private UserFlags getFlags() {
    return plugin.getUserService().getFlags();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("list")
            .then(argument("player", Arguments.USER)
                .executes(this::listFlags)
            )
        )

        .then(literal("set")
            .then(argument("player", Arguments.USER)
                .then(argument("flag", StringArgumentType.greedyString())
                    .executes(this::setFlag)
                )
            )
        )

        .then(literal("unset")
            .then(argument("player", Arguments.USER)
                .then(argument("flag", StringArgumentType.greedyString())
                    .suggests(this::suggestPlayerFlags)
                    .executes(this::unsetFlag)
                )
            )
        )

        .then(literal("clear").then(argument("player", Arguments.USER)
            .executes(this::clearFlags)
        ));
  }

  private int clearFlags(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    UserFlags flags = getFlags();

    flags.clearFlags(user.getUniqueId());

    c.getSource().sendSuccess(
        Messages.render("cmd.userflags.cleared")
            .addValue("player", user)
            .create(c.getSource())
    );

    return SINGLE_SUCCESS;
  }

  private CompletableFuture<Suggestions> suggestPlayerFlags(
      CommandContext<CommandSource> c,
      SuggestionsBuilder b
  ) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    UserFlags flags = getFlags();

    return Completions.suggest(b, flags.getFlags(user.getUniqueId()));
  }

  private int unsetFlag(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    UserFlags flags = getFlags();
    String flag = StringArgumentType.getString(c, "flag");

    if (!flags.unsetFlag(user.getUniqueId(), flag)) {
      throw Messages.render("cmd.userflags.errors.nothingChanged")
          .exception(c.getSource());
    }

    c.getSource().sendSuccess(
        Messages.render("cmd.userflags.unset")
            .addValue("player", user)
            .addValue("flag", flag)
            .create(c.getSource())
    );
    return SINGLE_SUCCESS;
  }

  private int setFlag(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    UserFlags flags = getFlags();
    String flag = StringArgumentType.getString(c, "flag");

    if (!flags.setFlag(user.getUniqueId(), flag)) {
      throw Messages.render("cmd.userflags.errors.nothingChanged")
          .exception(c.getSource());
    }

    c.getSource().sendSuccess(
        Messages.render("cmd.userflags.set")
            .addValue("player", user)
            .addValue("flag", flag)
            .create(c.getSource())
    );
    return SINGLE_SUCCESS;
  }

  private int listFlags(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    CommandSource source = c.getSource();

    UserFlags flags = getFlags();
    Set<String> flagSet = flags.getFlags(user.getUniqueId());

    if (flagSet.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    TextWriter writer = TextWriters.newWriter();
    writer.line(
        Messages.render("cmd.userflags.list.header")
            .addValue("player", user)
            .create(source)
    );

    for (String s : flagSet) {
      writer.line(
          Messages.render("cmd.userflags.list.entry")
              .addValue("flag", s)
      );
    }

    source.sendMessage(writer.asComponent());
    return SINGLE_SUCCESS;
  }
}
