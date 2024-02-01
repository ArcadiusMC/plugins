package net.arcadiusmc.core.commands.admin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.arcadiusmc.InventoryStorage;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.InventoryStorageImpl;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandInvStore extends BaseCommand {

  public CommandInvStore() {
    super("InvStore");

    setPermission(CorePermissions.CMD_INVSTORE);
    setDescription("Lets you give players separate inventories");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage(
        "reload",
        "Reloads stored inventories"
    );

    factory.usage(
        "save",
        "Saves all currently stored inventories to disk"
    );

    factory.usage(
        "save <player> <category: quoted string> [-keep-items]",

        "Saves a <player>'s inventory in a <category>",
        "If the '-keep-items' flag is not set, the player's",
        "inventory is cleared after it's stored away"
    );

    factory.usage(
        "return <player> <category: quoted string>",

        "Returns all items a <player> has stored in a <category>",
        "This command will also remove the items from storage"
    );

    factory.usage(
        "swap <player> <category: quoted string>",

        "Swaps a <player>'s current inventory with the one stored in",
        "<category>. If the player has no stored inventory in <category>",
        "then no items are returned.",
        "In either case, the player's current items are saved in",
        "the <category>"
    );

    factory.usage(
        "give <player> <category: quoted string>",

        "Works like the 'return' argument, except, it doesn't",
        "remove the items from storage"
    );
  }

  private static final SuggestionProvider<CommandSource> SUGGEST_CATEGORIES
      = (context, builder) -> {
          var user = Arguments.getUser(context, "user");
          var categories = InventoryStorage.getInstance()
              .getExistingCategories(user.getPlayer())
              .stream()
              .map(CommandInvStore::wrapIfNeeded);

          return Completions.suggest(builder, categories);
        };

  private static String wrapIfNeeded(String s) {
    for (var c: s.toCharArray()) {
      if (!StringReader.isAllowedInUnquotedString(c)) {
        return "'" + s + "'";
      }
    }

    return s;
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("reload")
            .executes(c -> {
              InventoryStorageImpl.getStorage().load();

              CommandSource source = c.getSource();
              source.sendSuccess(Messages.MESSAGE_LIST.renderText("cmd.invstore.reload", source));

              return 0;
            })
        )

        .then(literal("save")
            .executes(c -> {
              InventoryStorageImpl.getStorage().save();

              CommandSource source = c.getSource();
              source.sendSuccess(Messages.MESSAGE_LIST.renderText("cmd.invstore.saved", source));

              return 0;
            })

            .then(argument("user", Arguments.ONLINE_USER)
                .then(argument("category", StringArgumentType.string())
                    .suggests(SUGGEST_CATEGORIES)

                    .executes(c -> storeInventory(c, true))

                    .then(literal("-keep-items")
                        .executes(c -> storeInventory(c, false))
                    )
                )
            )
        )

        .then(literal("return")
            .then(argument("user", Arguments.ONLINE_USER)
                .then(argument("category", StringArgumentType.string())
                    .suggests(SUGGEST_CATEGORIES)
                    .executes(this::returnItems)
                )
            )
        )

        .then(literal("give")
            .then(argument("user", Arguments.ONLINE_USER)
                .then(argument("category", StringArgumentType.string())
                    .suggests(SUGGEST_CATEGORIES)
                    .executes(c -> giveItems(c))
                )
            )
        )

        .then(literal("swap")
            .then(argument("category", StringArgumentType.greedyString())
                .suggests(SUGGEST_CATEGORIES)
                .executes(c -> storeAndReturn(c))
            )
        );
  }

  static MessageRender alreadyStored(User player, String category) {
    return createMessage("cmd.invstore.error.taken", player, category);
  }

  static MessageRender nothingStored(User player, String category) {
    return createMessage("cmd.invstore.error.noStored", player, category);
  }

  static MessageRender createMessage(String key, User player, String category) {
    return Messages.MESSAGE_LIST.render(key)
        .addValue("player", player)
        .addValue("category", category);
  }

  private int storeInventory(CommandContext<CommandSource> c, boolean clearAfter)
      throws CommandSyntaxException
  {
    InventoryStorage store = InventoryStorage.getInstance();

    User user = getUser(c);
    String category = c.getArgument("category", String.class);

    if (store.hasStoredInventory(user.getPlayer(), category)) {
      throw alreadyStored(user, category).exception(c.getSource());
    }

    store.storeInventory(user.getPlayer(), category);

    if (clearAfter) {
      user.getInventory().clear();
    }

    CommandSource source = c.getSource();
    source.sendSuccess(
        createMessage("cmd.invstore.stored", user, category)
            .create(source)
    );
    return 0;
  }

  private int returnItems(CommandContext<CommandSource> c) throws CommandSyntaxException {
    InventoryStorage store = InventoryStorage.getInstance();

    User user = getUser(c);
    String category = c.getArgument("category", String.class);

    if (!store.hasStoredInventory(user.getPlayer(), category)) {
      throw nothingStored(user, category).exception(c.getSource());
    }

    store.returnItems(user.getPlayer(), category);

    CommandSource source = c.getSource();
    source.sendSuccess(
        createMessage("cmd.invstore.returned", user, category)
            .create(source)
    );
    return 0;
  }

  private int storeAndReturn(CommandContext<CommandSource> c) throws CommandSyntaxException {
    InventoryStorage store = InventoryStorage.getInstance();

    User user = getUser(c);
    String category = c.getArgument("category", String.class);

    store.swap(user.getPlayer(), category);

    CommandSource source = c.getSource();
    source.sendSuccess(
        createMessage("cmd.invstore.swapped", user, category)
            .create(source)
    );
    return 0;
  }

  private int giveItems(CommandContext<CommandSource> c) throws CommandSyntaxException {
    InventoryStorage store = InventoryStorage.getInstance();

    User user = getUser(c);
    String category = c.getArgument("category", String.class);

    if (!store.giveItems(user.getPlayer(), category)) {
      throw nothingStored(user, category).exception(c.getSource());
    }

    CommandSource source = c.getSource();
    source.sendSuccess(
        createMessage("cmd.invstore.give", user, category)
            .create(source)
    );
    return 0;
  }

  private User getUser(CommandContext<CommandSource> c) throws CommandSyntaxException {
    return Arguments.getUser(c, "user");
  }
}