package net.arcadiusmc.core.commands.admin;

import static net.arcadiusmc.Cooldowns.NO_END_COOLDOWN;
import static net.arcadiusmc.Cooldowns.TRANSIENT_CATEGORY;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;

public class CommandCooldown extends BaseCommand {

  public CommandCooldown() {
    super("Cooldown");

    setDescription("Controls various cooldowns");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    var prefixed = factory.withPrefix("<user> <category>");

    prefixed.usage("")
        .addInfo("Shows how long a <user> is on cooldown in a <category>");

    prefixed.usage("add [<length>]")
        .addInfo("Adds a <user> into a <category>'s cooldown.")
        .addInfo("If <length> is not set, the cooldown will never end");

    prefixed.usage("remove")
        .addInfo("Removes a <user> from a cooldown in a <category>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("user", Arguments.USER)
        .then(argument("category", StringArgumentType.string())
            .suggests((context, builder) -> {
              var input = builder.getRemainingLowerCase();

              if (Completions.matches(input, TRANSIENT_CATEGORY)) {
                builder.suggest(
                    TRANSIENT_CATEGORY,
                    () -> "Transient category that does NOT get saved"
                );
              }

              return Completions.suggest(
                  builder,
                  Cooldowns.cooldowns().getExistingCategories()
              );
            })


            // Test if on cooldown, show remaining duration
            .executes(this::showData)

            .then(literal("add")
                .executes(c -> add(c, true))

                .then(argument("length", ArgumentTypes.time())
                    .executes(c -> add(c, false))
                )
            )

            .then(literal("remove")
                .executes(this::remove)
            )
        )
    );
  }

  static MessageRender noCooldown(User player, String category) {
    return Messages.render("cmd.cooldown.error.noCooldown")
        .addValue("player", player)
        .addValue("category", category);
  }

  private int showData(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "user");
    String category = c.getArgument("category", String.class);
    var cds = Cooldowns.cooldowns();

    Duration duration = cds.getRemainingTime(user.getUniqueId(), category);

    if (duration == null) {
      throw noCooldown(user, category).exception(c.getSource());
    }

    MessageRender render = Messages.render(
        "cmd.cooldown.get." + (duration.getSeconds() < 0 ? "endless" : "finite")
    );

    c.getSource().sendMessage(
        render
            .addValue("player", user)
            .addValue("category", category)
            .addValue("remaining", duration)
            .create(c.getSource())
    );

    return 0;
  }

  private int add(CommandContext<CommandSource> c, boolean neverEnding)
      throws CommandSyntaxException
  {
    User user = Arguments.getUser(c, "user");
    String category = c.getArgument("category", String.class);

    long length;

    if (neverEnding) {
      length = NO_END_COOLDOWN;
    } else {
      length = ArgumentTypes.getMillis(c, "length");
    }

    var cds = Cooldowns.cooldowns();

    if (cds.onCooldown(user.getUniqueId(), category)) {
      throw Messages.render("cmd.cooldown.error.alreadySet")
          .addValue("player", user)
          .addValue("category", category)
          .exception(c.getSource());
    }

    cds.cooldown(user.getUniqueId(), category, length);

    MessageRender render = Messages.render(
        "cmd.cooldown.set." + (length == NO_END_COOLDOWN ? "endless" : "finite")
    );

    c.getSource().sendSuccess(
        render
            .addValue("time", Duration.ofMillis(length))
            .addValue("player", user)
            .addValue("category", category)
            .create(c.getSource())
    );

    return 0;
  }

  private int remove(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "user");
    String category = c.getArgument("category", String.class);
    var cds = Cooldowns.cooldowns();

    if (!cds.remove(user.getUniqueId(), category)) {
      throw noCooldown(user, category).exception(c.getSource());
    }

    c.getSource().sendSuccess(
        Messages.render("cmd.cooldown.remove")
            .addValue("player", user)
            .addValue("category", category)
            .create(c.getSource())
    );

    return 0;
  }
}