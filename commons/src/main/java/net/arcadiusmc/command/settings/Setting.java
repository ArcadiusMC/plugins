package net.arcadiusmc.command.settings;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.util.Objects;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.UserClickCallback;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickCallback.Options;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.permissions.Permission;

@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Setting {

  private final SettingAccess access;

  private SettingValidator validator = SettingValidator.NOP;
  private String messageKey;

  @Setter(AccessLevel.PRIVATE)
  @Getter(AccessLevel.PRIVATE)
  private BookSetting<User> setting;

  private String permission = null;

  private BaseCommand command;

  public static Setting create(SettingAccess access) {
    return new Setting(access);
  }

  public static Setting createInverted(SettingAccess access) {
    return create(access.negate());
  }

  public static Setting create(UserProperty<Boolean> property) {
    return create(SettingAccess.property(property));
  }

  public static Setting createInverted(UserProperty<Boolean> property) {
    return createInverted(SettingAccess.property(property));
  }

  public Setting setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public Setting setPermission(Permission permission) {
    return setPermission(permission.getName());
  }

  public void toggleState(User user) throws CommandSyntaxException {
    boolean newState = !access.getState(user);
    setState(user, newState);
  }

  private MessageRender getToggleMessage(boolean state) {
    MessageList list = Messages.MESSAGE_LIST;
    String stateSuffix = state ? "on" : "off";

    if (list.hasMessage(messageKey + "." + stateSuffix)) {
      return Messages.render(messageKey, stateSuffix);
    }

    return Messages.render(messageKey + ".toggle");
  }

  private Component description() {
    if (!Messages.MESSAGE_LIST.hasMessage(messageKey + ".description")) {
      return null;
    }

    return Messages.renderText(messageKey + ".description");
  }

  public Component displayName(Audience viewer) {
    Component text = Messages.renderText(messageKey + ".name", viewer);
    Component desc = description();

    if (desc != null) {
      text = text.hoverEvent(desc);
    }

    return text;
  }

  public Component getButtonDescription(Audience viewer, boolean state) {
    String stateSuffix = state ? "on" : "off";
    String messageKey = this.messageKey + ".toggledesc";

    MessageList list = Messages.MESSAGE_LIST;

    if (list.hasMessage(messageKey + "." + stateSuffix)) {
      return list.renderText(messageKey + "." + stateSuffix, viewer);
    }

    return list.render(messageKey)
        .addValue("state", state)
        .addValue("onoff", state ? "on" : "off")
        .addValue("enable", state ? "enable" : "disable")
        .addValue("Enable", state ? "Enable" : "Disable")
        .create(viewer);
  }

  public void setState(User user, boolean newState) throws CommandSyntaxException {
    if (validator != null) {
      validator.test(user, newState);
    }

    access.setState(user, newState);

    Component message = getToggleMessage(newState)
        .addValue("now", newState ? "ow" : "o longer")
        .addValue("inow", newState ? "o longer" : "ow")
        .addValue("state", newState ? "Enabled" : "Disabled")
        .addValue("istate", newState ? "Disabled" : "Enabled")
        .create(user);

    user.sendMessage(message);
  }

  public void toggleOther(CommandSource source, User target) throws CommandSyntaxException {
    if (source.isPlayer() && target.getName().equals(source.textName())) {
      toggleState(target);
      return;
    }

    boolean newState = !access.getState(target);

    if (validator != null) {
      try {
        validator.test(target, newState);
      } catch (CommandSyntaxException exc) {
        Component newMessage = Messages.render("settings.errors.updateOther")
            .addValue("player", target)
            .addValue("reason", Exceptions.message(exc))
            .addValue("name", displayName(source))
            .create(source);

        throw Exceptions.create(newMessage);
      }
    }

    access.setState(target, newState);

    Component message = Messages.render("settings.updatedOther")
        .addValue("player", target)
        .addValue("name", displayName(source))
        .addValue("value", newState)
        .create(source);

    source.sendSuccess(message);
  }

  private ClickCallback<Audience> createCallback(boolean state, SettingsBook<User> book) {
    return (UserClickCallback) user -> {
      setState(user, state);
      book.open(user, user);
    };
  }

  public Setting createCommand(String name, String... aliases) {
    SettingCommand command = new SettingCommand(name, this);
    command.setPermission(permission);

    Component desc = description();
    if (desc != null) {
      String descString = Text.plain(desc);
      command.setDescription(descString);
    }

    command.setAliases(aliases);

    this.command = command;
    command.register();

    return this;
  }


  public BookSetting<User> toBookSettng() {
    Objects.requireNonNull(messageKey, "messageKey not set");

    if (setting != null) {
      return setting;
    }

    return setting = new BookSetting<>() {

      ClickEvent enableCallback;
      ClickEvent disableCallback;

      static final Options options = Options.builder()
          .uses(-1)
          .lifetime(Duration.ofDays(365))
          .build();

      @Override
      public Component displayName(Audience viewer) {
        return Setting.this.displayName(viewer);
      }

      @Override
      public Component createButtons(User context, Audience viewer) {
        boolean state = access.getState(context);

        if (enableCallback == null) {
          ClickCallback<Audience> callback = createCallback(true, getBook());
          this.enableCallback = ClickEvent.callback(callback, options);
        }

        if (disableCallback == null) {
          ClickCallback<Audience> callback = createCallback(false, getBook());
          this.disableCallback = ClickEvent.callback(callback, options);
        }

        Component enable = createButton(
            true,
            state,
            enableCallback,
            getButtonDescription(viewer, true)
        );

        Component disable = createButton(
            false,
            state,
            disableCallback,
            getButtonDescription(viewer, false)
        );

        return Component.textOfChildren(enable, Component.space(), disable);
      }

      @Override
      public boolean shouldInclude(User context) {
        if (Strings.isNullOrEmpty(permission)) {
          return true;
        }

        return context.hasPermission(permission);
      }
    };
  }

  public interface SettingValidator {

    SettingValidator NOP = (user, newState) -> {};

    void test(User user, boolean newState) throws CommandSyntaxException;
  }
}