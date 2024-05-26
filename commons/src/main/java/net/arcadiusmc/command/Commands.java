package net.arcadiusmc.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.StackWalker.Option;
import java.util.Collection;
import java.util.Map;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.ExpandedEntityArgument;
import net.arcadiusmc.command.arguments.UserParseResult;
import net.arcadiusmc.command.arguments.chat.MessageArgument.Result;
import net.arcadiusmc.command.help.HelpListSyntaxConsumer;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.page.PagedIterator;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.Readers;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext.DefaultExecutionRule;
import net.forthecrown.grenadier.annotations.ArgumentModifier;
import net.forthecrown.grenadier.annotations.CommandDataLoader;
import net.forthecrown.grenadier.annotations.TypeRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class Commands {
  private Commands() {}

  public static final String DEFAULT_PERMISSION_FORMAT = "arcadius.commands.{command}";

  public static AnnotatedCommandContext createAnnotationContext() {
    AnnotatedCommandContext ctx = AnnotatedCommandContext.create();

    ctx.setDefaultRule(DefaultExecutionRule.IF_NO_CHILDREN);
    ctx.setDefaultPermissionFormat(DEFAULT_PERMISSION_FORMAT);
    ctx.setDefaultExecutes("execute");
    ctx.setTypeRegistry(createTypeRegistry());
    ctx.setSyntaxConsumer(new HelpListSyntaxConsumer());

    Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
    CommandDataLoader loader = CommandDataLoader.resources(caller.getClassLoader());
    ctx.addLoader(loader);

    Map<String, Object> variables = ctx.getVariables();

    ArgumentModifier<UserParseResult, User> resultToUser = (context, input) -> {
      return input.get(context.getSource(), true);
    };
    ArgumentModifier<UserParseResult, Collection<User>> resultToUsers = (context, input) -> {
      return input.getUsers(context.getSource(), true);
    };
    ArgumentModifier<Holder, Object> holderToValue = (context, input) -> input.getValue();
    ArgumentModifier<Result, PlayerMessage> resultToMessage = (context, input) -> {
      return input.toPlayerMessage(context.getSource().asBukkit());
    };
    ArgumentModifier<Result, PlayerMessage> resultToAdminMessage = (context, input) -> {
      return input.toPlayerMessage();
    };

    variables.put("result_to_message", resultToMessage);
    variables.put("result_to_admin_message", resultToAdminMessage);
    variables.put("result_to_user", resultToUser);
    variables.put("holder_to_value", holderToValue);
    variables.put("result_to_users", resultToUsers);

    return ctx;
  }

  public static TypeRegistry createTypeRegistry() {
    TypeRegistry registry = TypeRegistry.newRegistry();

    registry.register("user",         () -> Arguments.USER);
    registry.register("users",        () -> Arguments.USERS);
    registry.register("online_user",  () -> Arguments.ONLINE_USER);
    registry.register("online_users", () -> Arguments.ONLINE_USERS);
    registry.register("chat",         () -> Arguments.CHAT);
    registry.register("message",      () -> Arguments.MESSAGE);
    registry.register("rkey",         () -> Arguments.RESOURCE_KEY);
    registry.register("money",        () -> Arguments.MONEY);

    registry.register("f_player",     () -> new ExpandedEntityArgument(false, true));
    registry.register("f_players",    () -> new ExpandedEntityArgument(true, true));
    registry.register("f_entity",     () -> new ExpandedEntityArgument(false, false));
    registry.register("f_entities",   () -> new ExpandedEntityArgument(true, false));

    return registry;
  }

  public static String getDefaultPermission(String commandName) {
    return DEFAULT_PERMISSION_FORMAT.replace("{command}", commandName);
  }

  public static void ensureIndexValid(int index, int size) throws CommandSyntaxException {
    if (index < 1) {
      throw Exceptions.format("Index {0} is less than 1", index);
    }
    if (index > size) {
      throw Exceptions.invalidIndex(index, size);
    }
  }

  public static void ensurePageValid(int page, int pageSize, int size)
      throws CommandSyntaxException
  {
    if (size == 0) {
      throw Exceptions.NOTHING_TO_LIST.exception();
    }

    var max = PagedIterator.getMaxPage(pageSize, size);

    if (page >= max) {
      throw Exceptions.invalidPage(page + 1, max);
    }
  }

  public static ItemStack getHeldItem(Player player) throws CommandSyntaxException {
    var item = player.getInventory().getItemInMainHand();

    if (ItemStacks.isEmpty(item)) {
      throw Exceptions.MUST_HOLD_ITEM;
    }

    return item;
  }

  public static String optionallyQuote(String quote, String s) {
    for (var c: s.toCharArray()) {
      if (!StringReader.isAllowedInUnquotedString(c)) {
        return quote + s + quote;
      }
    }

    return s;
  }

  /**
   * Skips the given string in the given reader, if the given reader's remaining input starts with
   * the given string.
   *
   * @param reader The reader to move the cursor of
   * @param s      The string to skip
   */
  public static void skip(StringReader reader, String s) {
    if (!Readers.startsWithIgnoreCase(reader, s)) {
      return;
    }

    reader.setCursor(reader.getCursor() + s.length());
  }

  /**
   * Ensures that the given string reader is at the end of its input
   *
   * @param reader The reader to test
   * @throws CommandSyntaxException If the reader is not at the end of it's input
   */
  public static void ensureCannotRead(StringReader reader) throws CommandSyntaxException {
    if (reader.canRead()) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
          .dispatcherUnknownArgument()
          .createWithContext(reader);
    }
  }

  /**
   * Gets the specified {@code source} as a {@link User}
   * @param source Command source
   * @return Source's user object
   * @throws CommandSyntaxException If the {@code source} is not a player
   */
  public static User getUserSender(CommandSource source) throws CommandSyntaxException {
    return Users.get(source.asPlayer());
  }

  /**
   * Executes a specified command as the server's console. If this function is called
   * from an async context, then the bukkit scheduler is used to run the command
   * synchronously
   *
   * @param format The command format
   * @param args Arguments to format
   */
  public static void executeConsole(String format, Object... args) {
    execute(Grenadier.createSource(Bukkit.getConsoleSender()), format, args);
  }

  /**
   * Executes a specified command as the specified sender. If this function is called
   * from an async context, then the bukkit scheduler is used to run the command
   * synchronously
   *
   * @param source The source executing the command
   * @param format The command format
   * @param args Arguments to format
   */
  public static void execute(CommandSource source, String format, Object... args) {
    String command = String.format(format, args);

    if (Bukkit.isPrimaryThread()) {
      try {
        Grenadier.dispatch(source, command);
      } catch (Throwable t) {
        Loggers.getLogger().error("Error executing command '{}'", command, t);
      }
    } else {
      Plugin plugin = PluginUtil.getCallingPlugin();
      Bukkit.getScheduler().runTask(plugin, () -> {
        Grenadier.dispatch(source, command);
      });
    }
  }

  public static void removeChild(CommandNode<?> node, String name) {
    node.removeCommand(name);
  }

  /**
   * Delegate for {@link #replacePlaceholders(String, User)}
   *
   * @param command Base command string
   * @param player Player
   *
   * @return Formatted command string
   */
  public static String replacePlaceholders(@NotNull String command, @NotNull Player player) {
    return replacePlaceholders(command, Users.get(player));
  }

  /**
   * Replaces command placeholders in the specified {@code command}.
   * <p>
   *
   * Placeholders: <table>
   *  <tr><th>Placeholder</th><th>Description</th></tr>
   *  <tr><td>%player</td>Player's name</td></tr>
   *  <tr><td>%player.block</td> <td>Player's block position, eg: 1 2 3</td></tr>
   *  <tr><td>%player.pos</td> <td>Player's position, eg: 1.3242 1.421 5.2300023</td></tr>
   *  <tr><td>%player.worldkey</td> <td>Key of the player's world, eg: minecraft:overworld</td></tr>
   *  <tr><td>%player.world</td> <td>Name of the player's world, eg: world_the_end</td></tr>
   *  <tr><td>%player.x</td> <td>Player's X coordinate, eg: 1.343452</td></tr>
   *  <tr><td>%player.y</td> <td>Player's Y coordinate, eg: 1.000564</td></tr>
   *  <tr><td>%player.z</td> <td>Player's Z coordinate, eg: -546.59674</td></tr>
   *  <tr><td>%player.bx</td> <td>Player's X block coordinate, eg: 1</td></tr>
   *  <tr><td>%player.by</td> <td>Player's X block coordinate, eg: 546</td></tr>
   *  <tr><td>%player.bz</td> <td>Player's X block coordinate, eg: 25551</td></tr>
   *  <tr><td>%player.yaw</td> <td>Player's yaw rotation, eg: 90</td></tr>
   *  <tr><td>%player.pitch</td> <td>Player's pitch rotation, eg: 90</td></tr>
   *  <tr><td>%player.uuid</td> <td>Player's UUID, eg: ff09eaf1-b01e-4a17-a6f9-81468ce42f3a</td></tr>
   * </table>
   *
   * @param command Base command string
   * @param player Player
   *
   * @return Formatted command string
   */
  public static String replacePlaceholders(@NotNull String command, @NotNull User player) {
    Location l = player.getLocation();

    String block = String.format("%s %s %s", l.getBlockX(), l.getBlockY(), l.getBlockZ());
    String position = String.format("%s %s %s", l.getX(), l.getY(), l.getZ());

    return command
        .replace("%player.block", block)
        .replace("%player.pos", position)
        .replace("%player.worldkey", l.getWorld().getKey() + "")
        .replace("%player.world", l.getWorld().getName())
        .replace("%player.x", l.getX() + "")
        .replace("%player.y", l.getY() + "")
        .replace("%player.z", l.getZ() + "")
        .replace("%player.bx", l.getBlockX() + "")
        .replace("%player.by", l.getBlockY() + "")
        .replace("%player.bz", l.getBlockZ() + "")
        .replace("%player.yaw", l.getYaw() + "")
        .replace("%player.pitch", l.getPitch() + "")
        .replace("%player.uuid", player.getUniqueId() + "")
        .replace("%player", player.getName());
  }
}