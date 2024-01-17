package net.arcadiusmc.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.currency.Currency;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.format.NamedTextColor;

public class CurrencyCommand extends BaseCommand {

  private final Currency currency;

  public CurrencyCommand(
      String name,
      Currency currency,
      String... aliases
  ) {
    super(name);

    this.currency = currency;

    setAliases(aliases);
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /<name>
   * /<name> <user>
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    String units = currency.name();

    factory.usage("")
        .addInfo("Shows you your " + units);

    factory.usage("<player>")
        .addInfo("Shows you <player>'s " + units);

    factory.usage("add <player> <amount: number>")
        .setPermission(getAdminPermission())
        .addInfo("Adds <amount> of %s to <player>", units);

    factory.usage("set <player> <amount: number>")
        .setPermission(getAdminPermission())
        .addInfo("Sets the %s of <player> to <amount>", units);

    factory.usage("remove <player> <amount: number>")
        .setPermission(getAdminPermission())
        .addInfo("Removes <amount> from the %s of <player>", units);

    factory.usage("delete <player>")
        .setPermission(getAdminPermission())
        .addInfo("Deletes the <player>'s %s data", units);
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /<command>
        .executes(c -> lookup(c.getSource(), getUserSender(c)))

        // /<command> <user>
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              var user = Arguments.getUser(c, "user");
              return lookup(c.getSource(), user);
            })
        )

        // /<command> add <user> <amount>
        .then(literal("add")
            .requires(source -> source.hasPermission(getAdminPermission()))

            .then(argument("user", Arguments.USER)
                .then(argument("amount", IntegerArgumentType.integer(1))
                    .executes(c -> {
                      var user = Arguments.getUser(c, "user");
                      int amount = c.getArgument("amount", Integer.class);

                      currency.add(user.getUniqueId(), amount);
                      int newAmount = currency.get(user.getUniqueId());

                      c.getSource().sendSuccess(
                          Text.format("Added &e{0}&r to &6{1, user}&r's {2}, new value: &e{3}&r.",
                              NamedTextColor.GRAY,
                              currency.format(amount),
                              user,
                              getName(),
                              currency.format(newAmount)
                          )
                      );
                      return 0;
                    })
                )
            )
        )

        // /<command> subtract <user> <amount>
        .then(literal("subtract")
            .requires(source -> source.hasPermission(getAdminPermission()))

            .then(argument("user", Arguments.USER)
                .then(argument("amount", IntegerArgumentType.integer(1))
                    .executes(c -> {
                      var user = Arguments.getUser(c, "user");
                      int amount = c.getArgument("amount", Integer.class);

                      currency.remove(user.getUniqueId(), amount);
                      int newAmount = currency.get(user.getUniqueId());

                      c.getSource().sendSuccess(
                          Text.format("Subtracted &e{0}&r from &6{1, user}&r's {2}, new value: &e{3}&r.",
                              NamedTextColor.GRAY,
                              currency.format(amount),
                              user,
                              getName(),
                              currency.format(newAmount)
                          )
                      );
                      return 0;
                    })
                )
            )
        )

        // /<command> set <user> <amount>
        .then(literal("set")
            .requires(source -> source.hasPermission(getAdminPermission()))

            .then(argument("user", Arguments.USER)
                .then(argument("amount", IntegerArgumentType.integer(0))
                    .executes(c -> {
                      var user = Arguments.getUser(c, "user");
                      int amount = c.getArgument("amount", Integer.class);

                      int oldAmount = currency.get(user.getUniqueId());
                      currency.set(user.getUniqueId(), amount);
                      int newAmount = currency.get(user.getUniqueId());

                      c.getSource().sendSuccess(
                          Text.format("Set &e{0, user}&r's {1} to &6{2}&r. &7(Was {3})",
                              NamedTextColor.GRAY,
                              user,
                              getName(),
                              currency.format(newAmount),
                              currency.format(oldAmount)
                          )
                      );
                      return 0;
                    })
                )
            )
        );
  }

  private int lookup(CommandSource source, User user) {
    var val = currency.get(user.getUniqueId());
    var self = source.textName().equals(user.getName());

    if (self) {
      source.sendMessage(Messages.unitQuerySelf(currency.format(val)));
    } else {
      source.sendMessage(Messages.unitQueryOther(currency.format(val), user));
    }

    return 0;
  }
}