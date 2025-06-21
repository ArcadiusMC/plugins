package net.arcadiusmc.text;

import static net.arcadiusmc.Cooldowns.NO_END_COOLDOWN;
import static net.arcadiusmc.text.Text.format;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;

import com.google.common.base.Joiner;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;

public interface Messages {

  MessageList MESSAGE_LIST = MessageList.create();

  Style CHAT_URL = Style.style(TextDecoration.UNDERLINED)
      .hoverEvent(text("Click to open link!"));

  MessageRef BUTTON_ACCEPT_TICK = reference("generic.button.tick");

  MessageRef BUTTON_DENY_CROSS = reference("generic.button.cross");

  /**
   * Standard " < " previous page button with hover text, and bold and yellow styling
   */
  MessageRef PREVIOUS_PAGE = reference("generic.button.previousPage");

  /**
   * Standard " > " next page button with hover text, and bold and yellow styling
   */
  MessageRef NEXT_PAGE = reference("generic.button.nextPage");

  MessageRef BUTTON_CONFIRM = reference("generic.button.confirm");

  MessageRef BUTTON_DENY = reference("generic.button.deny");

  /**
   * Green button which states "[Acccept]" and has "Click to accept" as the hover text
   */
  MessageRef BUTTON_ACCEPT = reference("generic.button.accept");

  /**
   * Common text which says "Click me!" lol
   */
  MessageRef CLICK_ME = reference("generic.click_me");

  /**
   * Uncategorized message which states that all-caps messages cannot be sent
   */
  MessageRef ALL_CAPS = reference("allCaps");

  /**
   * This is for a weird thing to get around an issue, there are some commands that accept '-clear'
   * as a valid input for a message to clear some text or line somewhere, this allows us to still
   * use a message argument while testing if the input is meant to be a clear command or not
   */
  TextComponent DASH_CLEAR = text("-clear");

  /**
   * Text which simply says null
   */
  TextComponent NULL = text("null");

  MessageRef SEPARATED_FORMAT = reference("blocking.separated");
  MessageRef BLOCKED_SENDER   = reference("blocking.sender");
  MessageRef BLOCKED_TARGET   = reference("blocking.target");

  static MessageRender render(String key) {
    return MESSAGE_LIST.render(key);
  }

  static MessageRender render(String... key) {
    return MESSAGE_LIST.render(Joiner.on('.').join(key));
  }

  static Component renderText(String key) {
    return render(key).asComponent();
  }

  static Component renderText(String key, Audience viewer) {
    return render(key).create(viewer);
  }

  static MessageRef reference(String key) {
    return MESSAGE_LIST.reference(key);
  }

  static Component createButton(Component text, String cmd, Object... args) {
    return text.clickEvent(runCommand(String.format(cmd, args)));
  }

  /**
   * Creates a message which states that the given user is not online
   *
   * @param user The user whose display name to use
   * @return The formatted message
   */
  static ViewerAwareMessage notOnline(User user) {
    return render("player.notOnline").addValue("player", user);
  }

  /**
   * Creates a next page button by applying the given click event to the {@link #NEXT_PAGE}
   * constant.
   *
   * @param event The click event to apply, may be null
   * @return The created text
   */
  static Component nextPage(@Nullable ClickEvent event) {
    return NEXT_PAGE.renderText(null).clickEvent(event);
  }

  /**
   * Creates a previous page button by applying the given click event to the {@link #PREVIOUS_PAGE}
   * constant.
   *
   * @param event The click event to apply, may be null
   * @return The created text
   */
  static Component previousPage(@Nullable ClickEvent event) {
    return PREVIOUS_PAGE.renderText(null).clickEvent(event);
  }

  /**
   * Returns {@link #BUTTON_ACCEPT_TICK} with the given <code>cmd</code> as the
   * <code>run_command</code> click event
   *
   * @param cmd The command to use
   * @return The created button component
   */
  static Component tickButton(String cmd, Object... args) {
    return createButton(BUTTON_ACCEPT_TICK.renderText(null), cmd, args);
  }

  /**
   * Returns {@link #BUTTON_DENY_CROSS} with the given <code>cmd</code> as the
   * <code>run_command</code> click event
   *
   * @param cmd The command to use
   * @return The created button component
   */
  static Component crossButton(String cmd, Object... args) {
    return createButton(BUTTON_DENY_CROSS.renderText(null), cmd, args);
  }

  /**
   * Returns {@link #BUTTON_CONFIRM} with the given
   * <code>cmd</code> as the <code>run_command</code> click event
   *
   * @param cmd The command to use
   * @return The created button
   */
  static Component confirmButton(String cmd, Object... args) {
    return createButton(BUTTON_CONFIRM.renderText(null), cmd, args);
  }

  /**
   * Returns {@link #BUTTON_DENY} with the given
   * <code>cmd</code> as the <code>run_command</code> click event
   *
   * @param cmd The command to use
   * @return The created button
   */
  static Component denyButton(String cmd, Object... args) {
    return createButton(BUTTON_DENY.renderText(null), cmd, args);
  }

  /**
   * Returns {@link #BUTTON_ACCEPT} with the given
   * <code>cmd</code> as the <code>run_command</code> click event
   *
   * @param cmd The command to use
   * @return The created button
   */
  static Component acceptButton(String cmd, Object... args) {
    return createButton(BUTTON_ACCEPT.renderText(null), cmd, args);
  }

  /**
   * Creates a message stating the viewer died at the given location
   *
   * @param l The location the viewer died at
   * @return The formatted message
   */
  static ViewerAwareMessage diedAt(Location l) {
    return render("server.diedAt").addValue("location", l);
  }

  static Component chatMessage(Audience viewer, User sender, Component message) {
    return render("server.chat")
        .addValue("player", sender)
        .addValue("message", message)
        .create(viewer);
  }

  static Component chatMessage(Audience viewer, CommandSource sender, Component message) {
    return render("server.chat.nonPlayer")
        .addValue("sender", sender.displayName())
        .addValue("message", message)
        .create(viewer);
  }

  /**
   * Creates a message sent to the sender of a request.
   *
   * @param target The target of the request
   * @return The formatted message
   */
  static Component requestSent(User target, Component cancelButton) {
    return format("Sent request to &e{0, user}&r. &7{1}",
        NamedTextColor.GOLD,
        target,
        cancelButton
    );
  }


  /**
   * Creates an accept message for the request's sender telling them that the request's target has
   * accepted the request.
   *
   * @param target The Target that accepted the request
   * @return The formatted message
   */
  static Component requestAccepted(Component target) {
    return format("&e{0}&r accepted your request.",
        NamedTextColor.GOLD, target
    );
  }

  /**
   * Creates a denied message for the request's sender informing them that the request's target has
   * denied the request.
   *
   * @param target The user that denied the request
   * @return The formatted message
   */
  static Component requestDenied(Component target) {
    return format("&6{0}&r denied your request.",
        NamedTextColor.GRAY, target
    );
  }

  /**
   * Creates a cancellation message to tell the request's target that the sender cancelled the
   * request.
   *
   * @param sender The user that cancelled the request
   * @return The formatted message
   */
  static Component requestCancelled(User sender) {
    return requestCancelled(sender.displayName());
  }

  static Component requestCancelled(Component name) {
    return format("&6{0}&r cancelled their request",
        NamedTextColor.GRAY, name
    );
  }

  /**
   * Formats the message to tell users they have enabled/disabled a boolean
   * {@link UserProperty}
   * <p>
   * The format is given 2 of the following arguments, which changes depending
   * on the <code>state</code> parameter:<pre>
   * Argument 0: "Enabled" or "Disabled"
   * Argument 1: "ow" or "o longer"
   * Argument 2: "o longer" or "ow", same as above basically, but inverse
   * </pre>
   * The second argument doesn't have a starting 'n', this is so you can decide if that letter
   * should be capitalized yourself.
   * <p>
   * The color of the returned text also depends on the <code>state</code> parameter, if it's true,
   * the color will be yellow, otherwise it'll be gray
   *
   * @param format The message format to use.
   * @param state  The new state of the property
   * @return The formatted component
   */
  static Component toggleMessage(String format, boolean state) {
    return format(format,
        state ? NamedTextColor.YELLOW : NamedTextColor.GRAY,
        /* Arg 0 */ state ? "Enabled" : "Disabled",
        /* Arg 1 */ state ? "ow" : "o longer",
        /* Arg 2 */ state ? "o longer" : "ow",
        /* Arg3  */ state ? "Disabled" : "Enabled"
    );
  }

  /**
   * Creates a message for staff when they change a boolean
   * {@link UserProperty} for another user.
   *
   * @param display The setting's display name
   * @param user    The user the value was changed for
   * @param state   The new state of the property
   * @return The formatted component
   */
  static Component toggleOther(String display, User user, boolean state) {
    return format("{0} {1} for &e{2, user}",
        NamedTextColor.GRAY,

        !state ? "Disabled" : "Enabled",
        display, user
    );
  }

  static ViewerAwareMessage firstTimeJoin(User user) {
    return render("server.join.firstTime")
        .addValue("player", user);
  }

  static ViewerAwareMessage joinMessage(User user) {
    return render("server.join")
        .addValue("player", user);
  }

  static ViewerAwareMessage newNameJoinMessage(User user, Object previousName) {
    return render("server.join.newName")
        .addValue("player", user)
        .addValue("previousName", previousName);
  }

  static ViewerAwareMessage leaveMessage(User user, QuitReason reason) {
    String formatKey = "server.leave" + switch (reason) {
      case KICKED -> ".kicked";
      case ERRONEOUS_STATE -> ".error";
      case TIMED_OUT -> ".timeout";
      default -> "";
    };

    return render(formatKey)
        .addValue("player", user);
  }


  /**
   * Creates a message stating the user has the given text
   *
   * @param unitDisplay The unit display of the user
   * @return The formatted message
   */
  static ViewerAwareMessage unitQuerySelf(Component unitDisplay) {
    return render("unitQuery.self")
        .addValue("units", unitDisplay);
  }

  /**
   * Creates a message stating the given user has the given text.
   *
   * @param unitDisplay The unit display of the user
   * @param target      The user
   * @return The formatted message
   */
  static ViewerAwareMessage unitQueryOther(Component unitDisplay, User target) {
    return render("unitQuery.other")
        .addValue("units", unitDisplay)
        .addValue("player", target);
  }

  static ViewerAwareMessage clickToTeleport() {
    return render("generic.click_to_teleport");
  }

  static ViewerAwareMessage location(Location l, boolean includeWorld, boolean clickable) {
    String formatKey = "formats.location" + (includeWorld ? "" : ".noWorld");

    MessageRender message = render(formatKey).addValue("pos", l);

    if (!clickable) {
      return message;
    }

    return viewer -> {
      return message.create(viewer)
          .hoverEvent(clickToTeleport().create(viewer))
          .clickEvent(ClickEvent.runCommand(
              "/tp_exact x=%s y=%s z=%s yaw=%s pitch=%s world=%s".formatted(
                  l.getX(), l.getY(), l.getZ(),
                  l.getYaw(),
                  l.getPitch(),
                  l.getWorld().getName()
              )
          ));
    };
  }

  static Component currency(Number amount) {
    return UnitFormat.currency(amount);
  }

  static Component currencyUnit(boolean singular) {
    String formatKey = singular ? "units.currency.singular" : "units.currency.plural";
    return MESSAGE_LIST.renderText(formatKey, null);
  }

  static Component cooldownMessage(Audience viewer, Duration remaining, Duration cooldownLength) {
    MessageRender render;

    if (cooldownLength == null
        || cooldownLength.toMillis() == NO_END_COOLDOWN
        || remaining.isNegative()
    ) {
      render = MESSAGE_LIST.render("cooldowns.eternal");
    } else {
      boolean longCooldown = cooldownLength.toMillis() > TimeUnit.MINUTES.toMillis(10);
      render = MESSAGE_LIST.render(longCooldown ? "cooldowns.long" : "cooldowns.short");
    }

    return render
        .addValue("remaining", remaining)
        .addValue("cooldown", cooldownLength)
        .create(viewer);
  }

  static CommandSyntaxException tpNotAllowedHere(Audience viewer) {
    return render("errors.tpFromHere").exception(viewer);
  }
}