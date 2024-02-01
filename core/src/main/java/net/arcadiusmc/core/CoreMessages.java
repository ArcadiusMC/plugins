package net.arcadiusmc.core;

import static net.arcadiusmc.text.Messages.MESSAGE_LIST;
import static net.arcadiusmc.text.Messages.currency;
import static net.arcadiusmc.text.Text.format;
import static net.arcadiusmc.text.Text.isEmpty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.core.commands.CommandSelfOrUser;
import net.arcadiusmc.core.commands.CommandSuicide;
import net.arcadiusmc.core.user.UserHomes;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public interface CoreMessages {

  /**
   * The title header of the ignored players list
   */
  MessageRef IGNORE_LIST = MESSAGE_LIST.reference("cmd.ignorelist.format");

  /**
   * Suffix to display for players on the <code>/list</code> command that are hidden, aka,
   * vanished.
   */
  Component VANISHED_LIST_SUFFIX = text(" [Hidden]", NamedTextColor.GRAY);

  MessageRef NEARBY_FORMAT = MESSAGE_LIST.reference("cmd.near.format");

  MessageRef NEARBY_ENTRY = MESSAGE_LIST.reference("cmd.near.entry");

  MessageRef ME_OTHER = MESSAGE_LIST.reference("commands.me.other");

  MessageRef ME_SELF = MESSAGE_LIST.reference("commands.me.self");

  MessageRef PLAYER_LIST_FORMAT = MESSAGE_LIST.reference("cmd.list.message");

  /**
   * Message shown by {@link CommandSuicide}
   */
  MessageRef CMD_SUICIDE = MESSAGE_LIST.reference("commands.suicide");

  /**
   * Message stating the viewer was healed
   */
  MessageRef HEALED = MESSAGE_LIST.reference("commands.heal.target");

  /**
   * Message stating the viewer had their appetite satiated.
   */
  MessageRef FED = MESSAGE_LIST.reference("commands.fed.target");

  MessageRef HOPPER_WARNING = MESSAGE_LIST.reference("tooManyHoppers");

  MessageRef DURABILITY_WARN_SUBTITLE = MESSAGE_LIST.reference("durabilityWarning.subtile");

  MessageRef DURABILITY_WARN_TITLE = MESSAGE_LIST.reference("durabilityWarning.tile");

  /**
   * Inventory title used by {@link CommandSelfOrUser} for it's
   * <code>/bin</code> command
   */
  Component DISPOSAL = text("Disposal");

  MessageRef PAY_SELF = MESSAGE_LIST.reference("cmd.pay.error.self");

  MessageRef PAY_DISABLED_TARGET = MESSAGE_LIST.reference("cmd.pay.error.blocked");
  MessageRef PAY_DISABLED_SENDER = MESSAGE_LIST.reference("cmd.pay.error.disabled");

  /**
   * Lists the homes in the given home map. This only lists the entries, this does not include any
   * header
   *
   * @param homes The home map to list
   * @param cmd   The command prefix to use for the entry's {@link net.kyori.adventure.text.event.ClickEvent}s
   * @return The formatted message
   */
  static Component listHomes(UserHomes homes, String cmd) {
    return TextJoiner.onComma()
        .add(
            homes.getHomes().entrySet().stream()
                .map(entry -> {
                  return text("[" + entry.getKey() + "]", NamedTextColor.GOLD)
                      .hoverEvent(Text.prettyLocation(entry.getValue(), false))
                      .clickEvent(runCommand(cmd + entry.getKey()));
                })
        )
        .asComponent();
  }

  static Component withdrew(Audience viewer, int coins, int worth) {
    return coinMessage("withdraw", viewer, coins, worth);
  }

  static Component deposit(Audience viewer, int coins, int worth) {
    return coinMessage("deposit", viewer, coins, worth);
  }

  private static Component coinMessage(String key, Audience viewer, int coins, int worth) {
    String formatKey = coins == 1
        ? "coins." + key + ".single"
        : "coins." + key + ".multiple";

    return MESSAGE_LIST.render(formatKey)
        .addValue("coins", coins)
        .addValue("worth", currency(worth))
        .create(viewer);
  }

  /**
   * Lists all blocked users
   *
   * @param users The users to list
   * @return The formatted component
   */
  static Component listBlocked(Collection<UUID> users, Audience viewer) {
    return joinIds(users, IGNORE_LIST, viewer);
  }

  static Component joinIds(Collection<UUID> uuids, MessageRef format, Audience viewer) {
    Component joined = TextJoiner.onComma()
        .add(uuids.stream().map(uuid -> {
          var user = Users.get(uuid);
          return user.displayName(viewer).color(NamedTextColor.YELLOW);
        }))
        .asComponent();

    return format.get()
        .addValue("players", joined)
        .addValue("uuids", uuids)
        .create(viewer);
  }


  /**
   * Creates a message saying the given player was ignored
   *
   * @param target The player being ignored
   * @return The formatted message
   */
  static ViewerAwareMessage ignorePlayer(User target) {
    return MESSAGE_LIST.render("blocking.added")
        .addValue("player", target);
  }

  /**
   * Creates a message saying the given player was unignored
   *
   * @param target The player being unignored
   * @return The formatted message
   */
  static ViewerAwareMessage unignorePlayer(User target) {
    return MESSAGE_LIST.render("blocking.removed")
        .addValue("player", target);
  }

  /**
   * Creates a header for the <code>/list</code> command
   *
   * @param userCount The amount of online users
   * @return The formatted message
   */
  static Component listHeader(int userCount) {
    return format("There are &6{0, number}&r out of &6{1, number}&r players online.",
        NamedTextColor.YELLOW,
        userCount, Bukkit.getMaxPlayers()
    );
  }

  /**
   * Lists all players in the given collection. This will also prepend 'Players: ' onto the front of
   * that list.
   *
   * @param users The users to list, vanished or afk players will not be ignored
   * @return The formatted component
   */
  static Component listPlayers(Collection<User> users, Audience viewer) {
    return TextJoiner.onComma()
        .setColor(NamedTextColor.YELLOW)

        // Add users
        .add(users.stream()
            .map(user -> {
              var text = text()
                  .color(NamedTextColor.WHITE)
                  .append(user.displayName(viewer));

              if (user.get(Properties.VANISHED)) {
                text.append(VANISHED_LIST_SUFFIX);
              }

              return text.build();
            })
        )

        // Return result
        .asComponent();
  }

  MessageRef NICK_TOO_LONG = MESSAGE_LIST.reference("cmd.nickname.error.tooLong");
  MessageRef NICK_UNAVAILABLE = MESSAGE_LIST.reference("cmd.nickname.error.unavailable");
  MessageRef NICK_NONE_SET = MESSAGE_LIST.reference("cmd.nickname.error.noNick");
  MessageRef NICK_SET_SELF = MESSAGE_LIST.reference("cmd.nickname.set.self");
  MessageRef NICK_SET_OTHER = MESSAGE_LIST.reference("cmd.nickname.set.other");
  MessageRef NICK_CLEARED_SELF = MESSAGE_LIST.reference("cmd.nickname.cleared.self");
  MessageRef NICK_CLEARED_OTHER = MESSAGE_LIST.reference("cmd.nickname.cleared.other");

  /**
   * Creates a message saying the item held by the given user was repaired
   *
   * @param user The user holding the item
   * @return The formatted message
   */
  static Component repairedItem(User user) {
    return format("Repaired item held by &6{0, user}&r.", NamedTextColor.YELLOW, user);
  }

  /**
   * Creates a message saying the given user is being healed
   *
   * @param target The user being healed
   * @return The formatted message
   */
  static ViewerAwareMessage healing(User target) {
    return MESSAGE_LIST.render("commands.heal")
        .addValue("player", target);
  }

  /**
   * Creates a message saying the given user was fed
   *
   * @param target The user that was fed
   * @return The formatted message
   */
  static ViewerAwareMessage feeding(User target) {
    return MESSAGE_LIST.render("commands.fed")
        .addValue("player", target);
  }

  static ViewerAwareMessage paidMultiple(int paid, int amount) {
    return MESSAGE_LIST.render("cmd.pay.sender.total")
        .addValue("totalAmount", amount)
        .addValue("targetCount", paid);
  }

  static Component payTarget(
      Audience sender,
      Audience target,
      int amount,
      @Nullable Component message
  ) {
    return payMessage("target", sender, target, amount, message).create(target);
  }

  static Component paySender(
      Audience sender,
      Audience target,
      int amount,
      @Nullable Component message
  ) {
    return payMessage("sender", sender, target, amount, message).create(sender);
  }

  private static ViewerAwareMessage payMessage(
      String key,
      Audience sender,
      Audience target,
      int amount,
      Component message
  ) {
    MessageRender render;

    if (isEmpty(message)) {
      render = MESSAGE_LIST.render("cmd.pay." + key);
    } else {
      render = MESSAGE_LIST.render("cmd.pay." + key + ".message");
    }

    return render
        .addValue("sender", sender)
        .addValue("target", target)
        .addValue("amountRaw", amount)
        .addValue("amount", currency(amount))
        .addValue("message", message);
  }

  static List<Component> coinLore(int amount) {
    List<Component> lines = new ArrayList<>();
    Component currencyText = currency(amount);

    lines.add(
        MESSAGE_LIST.render("coins.lore.worth")
            .addValue("world", Worlds.overworld())
            .addValue("worth", currencyText)
            .addValue("rawWorth", amount)
            .create(null)
    );

    lines.add(
        MESSAGE_LIST.render("coins.lore.date")
            .addValue("world", Worlds.overworld())
            .addValue("worth", currencyText)
            .addValue("rawWorth", amount)
            .create(null)
    );

    return lines;
  }
}
