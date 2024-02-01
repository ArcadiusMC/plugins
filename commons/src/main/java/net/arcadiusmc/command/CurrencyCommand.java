package net.arcadiusmc.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.currency.Currency;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

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
    String units = currency.pluralName();

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

                      int oldAmount = currency.get(user.getUniqueId());
                      currency.add(user.getUniqueId(), amount);
                      int newAmount = currency.get(user.getUniqueId());

                      c.getSource().sendSuccess(
                          editMessage("added", amount, oldAmount, newAmount, user)
                              .create(c.getSource())
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

                      int oldAmount = currency.get(user.getUniqueId());
                      currency.remove(user.getUniqueId(), amount);
                      int newAmount = currency.get(user.getUniqueId());

                      c.getSource().sendSuccess(
                          editMessage("subtracted", amount, oldAmount, newAmount, user)
                              .create(c.getSource())
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
                          editMessage("set", amount, oldAmount, newAmount, user)
                              .create(c.getSource())
                      );
                      return 0;
                    })
                )
            )
        );
  }

  private MessageRender editMessage(
      String keySuffix,
      int amount,
      int old,
      int newAmount,
      User player
  ) {
    return Messages.render("unitEdit." + keySuffix)
        .addValue("player",   player)
        .addValue("currency", currency.pluralName())
        .addValue("amount",   currency.format(amount))
        .addValue("old",      currency.format(old))
        .addValue("new",      currency.format(newAmount));
  }

  private int lookup(CommandSource source, User user) {
    var val = currency.get(user.getUniqueId());
    var self = source.textName().equals(user.getName());

    if (self) {
      source.sendMessage(Messages.unitQuerySelf(currency.format(val)).create(source));
    } else {
      source.sendMessage(Messages.unitQueryOther(currency.format(val), user).create(source));
    }

    return 0;
  }
}