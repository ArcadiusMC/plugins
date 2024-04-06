package net.arcadiusmc.afk.commands;

import net.arcadiusmc.afk.AfkPlugin;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandAfk extends BaseCommand {

  private final AfkPlugin plugin;

  public CommandAfk(AfkPlugin plugin) {
    super("afk");

    this.plugin = plugin;

    setDescription("Marks or un-marks you as AFK");
    setAliases("away");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Sets you afk/unafk");
    factory.usage("<message>", "AFKs you with an AFK message");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> afk(getUserSender(c), null))

        .then(literal("reload-config")
            .requires(source -> source.hasPermission(getAdminPermission()))

            .executes(c -> {
              plugin.reloadConfig();

              c.getSource().sendSuccess(Messages.renderText("afk.reloaded.config"));
              return 0;
            })
        )

        .then(literal("toggle-other")
            .requires(s -> s.hasPermission(getAdminPermission()))

            .then(argument("user", Arguments.ONLINE_USER)
                .requires(s -> s.hasPermission(getAdminPermission()))

                .executes(c -> afk(
                    Arguments.getUser(c, "user"),
                    null
                ))

                .then(argument("message", Arguments.MESSAGE)
                    .requires(s -> s.hasPermission(getAdminPermission()))

                    .executes(c -> {
                      User user = Arguments.getUser(c, "user");
                      PlayerMessage message = Arguments.getPlayerMessage(c, "message");
                      return afk(user, message);
                    })
                )
            )
        )

        .then(argument("message", Arguments.MESSAGE)
            .executes(c -> afk(
                getUserSender(c),
                Arguments.getPlayerMessage(c, "message")
            ))
        );
  }

  private int afk(User user, PlayerMessage message) {
    plugin.getAfk().getState(user).ifPresent(state -> {
      if (state.isAfk()) {
        state.unafk();
      } else {
        state.afk(message);
      }
    });

    return 0;
  }
}