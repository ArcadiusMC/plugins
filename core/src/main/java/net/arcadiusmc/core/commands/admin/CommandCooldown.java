package net.arcadiusmc.core.commands.admin;

import static net.arcadiusmc.Cooldowns.NO_END_COOLDOWN;
import static net.arcadiusmc.Cooldowns.TRANSIENT_CATEGORY;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.format.NamedTextColor;

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

  private int showData(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "user");
    String category = c.getArgument("category", String.class);
    var cds = Cooldowns.cooldowns();

    Duration duration = cds.getRemainingTime(user.getUniqueId(), category);

    if (duration == null) {
      throw Exceptions.format("{0, user} is not on '{1}' cooldown",
          user, category
      );
    }

    if (duration.getSeconds() < 0) {
      c.getSource().sendMessage(
          Text.format("&e{0, user}&r's '&e{1}&r' cooldown will never end",
              NamedTextColor.GRAY,
              user, category
          )
      );
    } else {
      c.getSource().sendMessage(
          Text.format(
              "&e{0, user}&r '&e{1}&r' cooldown "
                  + "remaining: &e{2, time, -short -biggest}",

              NamedTextColor.GRAY,
              user, category, duration
          )
      );
    }

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
      throw Exceptions.format("{0, user} is already on cooldown for {1}",
          user, category
      );
    }

    cds.cooldown(user.getUniqueId(), category, length);

    c.getSource().sendSuccess(
        Text.format(
            "Placed &e{0, user}&r into cooldown, "
                + "category='&e{1}&r', "
                + "length=&e{2, time, -short -biggest}&r",
            NamedTextColor.GRAY,

            user, category,
            length == NO_END_COOLDOWN ? "Eternal" : length
        )
    );
    return 0;
  }

  private int remove(CommandContext<CommandSource> c) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "user");
    String category = c.getArgument("category", String.class);
    var cds = Cooldowns.cooldowns();

    if (!cds.remove(user.getUniqueId(), category)) {
      throw Exceptions.format("{0, user} is not on cooldown for {1}",
          user, category
      );
    }

    c.getSource().sendSuccess(
        Text.format("&e{0, user}&r was removed from the '&e{1}&r' cooldown",
            NamedTextColor.GRAY,
            user, category
        )
    );
    return 0;
  }
}