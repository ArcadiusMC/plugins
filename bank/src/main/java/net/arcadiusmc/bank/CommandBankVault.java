package net.arcadiusmc.bank;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.arcadiusmc.bank.BankPlugin.ENTER_ALREADY_IN_VAULT;
import static net.arcadiusmc.bank.BankPlugin.ENTER_UNKNOWN_WORLD;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandContexts;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.permissions.Permission;

public class CommandBankVault extends BaseCommand {

  static final Permission PERMISSION = Permissions.registerCmd("bankruns");

  private final BankPlugin plugin;

  public CommandBankVault(BankPlugin plugin) {
    super("bank-runs");

    this.plugin = plugin;

    setAliases("bankruns");
    setDescription("Bank vault admin command");
    setPermission(PERMISSION);

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage(
        "reload",
        "Reloads the plugin"
    );

    factory.usage(
        "list",
        "Lists all active bank runs"
    );

    factory.usage(
        "enter <player> <vault> [<variant>]",
        "Makes a player start a bank run"
    );

    factory.usage(
        "kick <player>",
        "Kicks a player from their current run, removing all their gains"
    );

    factory.usage(
        "finish <player>",
        "Completes a bank run for a player, letting them keep all their gains"
    );

  }

  @Override
  public void createCommand(GrenadierCommand command) {
    ArgumentType<BankVault> vaultArgumentType = ArgumentTypes.map(plugin.getVaultMap());

    command
        .then(literal("debug")
            .requires(source -> source.hasPermission(VaultDebug.DEBUG_PERMISSION))

            .then(literal("toggle-debug")
                .executes(c -> {
                  VaultDebug debug = plugin.getDebug();
                  boolean newState;

                  if (debug.isTicking()) {
                    debug.stopTicking();
                    newState = false;
                  } else {
                    debug.startTicking();
                    newState = true;
                  }

                  c.getSource().sendSuccess(
                      Messages.render("cmd.bankruns.debug.toggle")
                          .addValue("state", newState)
                          .create(c.getSource())
                  );
                  return SINGLE_SUCCESS;
                })
            )

            .then(createDebugToggle("drawChests", t -> VaultDebug.drawChests = t, () -> VaultDebug.drawChests))
            .then(createDebugToggle("drawCoins", t -> VaultDebug.drawCoins = t, () -> VaultDebug.drawCoins))
            .then(createDebugToggle("drawRoom", t -> VaultDebug.drawRoom = t, () -> VaultDebug.drawRoom))
        )

        .then(literal("reload")
            .executes(c -> {
              plugin.reloadConfig();

              c.getSource().sendSuccess(
                  Messages.renderText("cmd.bankruns.reload", c.getSource())
              );
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("list")
            .executes(this::listRuns)
        )

        .then(literal("enter")
            .then(argument("vault", vaultArgumentType)
                .then(argument("player", Arguments.ONLINE_USER)
                    .executes(c -> startRun(c, false))

                    .then(argument("variant", StringArgumentType.string())
                        .suggests((context, builder) -> {
                          BankVault vault = context.getArgument("vault", BankVault.class);
                          Set<String> variants = vault.getTable().getVariantTable().rowKeySet();

                          return Completions.suggest(builder, variants);
                        })

                        .executes(c -> startRun(c, true))
                    )
                )
            )
        )

        .then(literal("kick")
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> kick(c, false))
            )
        )

        .then(literal("finish")
            .then(argument("player", Arguments.ONLINE_USER)
                .executes(c -> kick(c, true))
            )
        );
  }

  private int kick(CommandContext<CommandSource> c, boolean success) throws CommandSyntaxException {
    User user = Arguments.getUser(c, "player");
    BankRun run = plugin.getSessionMap().get(user.getUniqueId());

    if (run == null) {
      throw Messages.render("cmd.bankruns.error.notInVault")
          .addValue("player", user)
          .exception(c.getSource());
    }

    run.kick(!success);

    c.getSource().sendSuccess(
        Messages.render("cmd.bankruns", success ? "finish" : "kick")
            .addValue("player", user)
            .create(c.getSource())
    );

    return SINGLE_SUCCESS;
  }

  private int listRuns(CommandContext<CommandSource> c) throws CommandSyntaxException {
    Map<UUID, BankRun> map = plugin.getSessionMap();
    if (map.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(c.getSource());
    }

    TextWriter writer = TextWriters.newWriter();
    writer.line(Messages.render("cmd.bankruns.list.header"));

    map.forEach((uuid, bankRun) -> {
      User user = Users.get(uuid);
      writer.line(
          Messages.render("cmd.bankruns.list.format")
              .addValue("player", user)
              .addValue("vault", bankRun.getVaultKey())
      );
    });

    writer.line(Messages.render("cmd.bankruns.list.footer"));

    c.getSource().sendMessage(writer.asComponent());
    return SINGLE_SUCCESS;
  }

  private int startRun(CommandContext<CommandSource> c, boolean variantSet)
      throws CommandSyntaxException
  {
    User user = Arguments.getUser(c, "player");
    BankVault vault = c.getArgument("vault", BankVault.class);
    String vaultKey = CommandContexts.getInput(c, "vault");
    String variant;

    if (variantSet) {
      variant = StringArgumentType.getString(c, "variant");
    } else {
      variant = "default";
    }

    int res = plugin.startRun(user, vault, vaultKey, variant);

    if (res == ENTER_ALREADY_IN_VAULT) {
      throw Messages.render("cmd.bankruns.error.alreadyInVault")
          .addValue("player", user)
          .exception(c.getSource());
    }
    if (res == ENTER_UNKNOWN_WORLD) {
      throw Messages.render("cmd.bankruns.error.unknownWorld")
          .addValue("vault", vaultKey)
          .addValue("player", user)
          .addValue("worldName", vault.getWorldName())
          .exception(c.getSource());
    }

    c.getSource().sendSuccess(
        Messages.render("cmd.bankruns.enter")
            .addValue("player", user)
            .addValue("vault", vaultKey)
            .create(c.getSource())
    );
    return SINGLE_SUCCESS;
  }

  private static LiteralArgumentBuilder<CommandSource> createDebugToggle(
      String name,
      BooleanConsumer toggle,
      BooleanSupplier getter
  ) {
    return literal(name)
        .executes(c -> {
          CommandSource source = c.getSource();
          boolean state = !getter.getAsBoolean();

          toggle.accept(state);

          source.sendSuccess(
              Messages.render("cmd.bankruns.debug")
                  .addValue("state", state)
                  .addValue("debugName", name)
                  .create(source)
          );
          return SINGLE_SUCCESS;
        });
  }
}
