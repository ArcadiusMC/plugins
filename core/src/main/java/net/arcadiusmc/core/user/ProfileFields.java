package net.arcadiusmc.core.user;

import static net.arcadiusmc.text.Messages.MESSAGE_LIST;
import static net.kyori.adventure.text.Component.text;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.function.Function;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.core.CoreMessages;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PeriodFormat;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserBlockList;
import net.arcadiusmc.user.name.DisplayContext;
import net.arcadiusmc.user.name.FieldPlacement;
import net.arcadiusmc.user.name.ProfileDisplayElement;
import net.arcadiusmc.user.name.UserNameFactory;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Location;

interface ProfileFields {

  ProfileDisplayElement UUID = new ProfileDisplayElement() {
    @Override
    public void write(TextWriter writer, User user, DisplayContext context) {
      writer.field(MESSAGE_LIST.render("profile.uuid"), user.getUniqueId());
    }

    @Override
    public FieldPlacement placement() {
      return FieldPlacement.IN_HOVER;
    }
  };

  ProfileDisplayElement REAL_NAME = (writer, user, context) -> {
    if (user.getNickname() == null && !user.has(Properties.TAB_NAME)) {
      return;
    }

    writer.field(MESSAGE_LIST.render("profile.realName"), user.getName());
  };

  ProfileDisplayElement FIRST_JOIN = timestamp("profile.firstJoin", TimeField.FIRST_JOIN);

  ProfileDisplayElement BALANCE = (writer, user, context) -> {
    int money = user.getBalance();

    if (money <= 0) {
      return;
    }

    writer.field(MESSAGE_LIST.render("profile.balances"), Messages.currency(money));
  };

  ProfileDisplayElement VOTES = (writer, user, context) -> {
    int votes = user.getTotalVotes();

    if (votes <= 0) {
      return;
    }

    writer.field(MESSAGE_LIST.render("profile.votes"), UnitFormat.votes(votes));
  };

  ProfileDisplayElement PLAYTIME = new ProfileDisplayElement() {
    @Override
    public void write(TextWriter writer, User user, DisplayContext context) {
      int playtime = user.getPlayTime();
      writer.field(
          MESSAGE_LIST.render("profile.playtime"),
          UnitFormat.playTime(playtime)
      );
    }

    @Override
    public FieldPlacement placement() {
      return FieldPlacement.ALL;
    }
  };

  ProfileDisplayElement PROFILE_PRIVATE_STATE = new ProfileDisplayElement() {
    @Override
    public void write(TextWriter writer, User user, DisplayContext context) {
      if (!context.self() && !context.viewerHasPermission(Permissions.PROFILE_BYPASS)) {
        return;
      }

      writer.field(MESSAGE_LIST.render("profile.status"), !user.get(Properties.PROFILE_PRIVATE));
    }

    @Override
    public FieldPlacement placement() {
      return FieldPlacement.ALL;
    }
  };

  ProfileDisplayElement LAST_ONLINE = new ProfileDisplayElement() {
    @Override
    public void write(TextWriter writer, User user, DisplayContext context) {
      if (user.isOnline()) {
        return;
      }

      long lastJoin = user.getTime(TimeField.LAST_LOGIN);

      writer.field(MESSAGE_LIST.render("profile.lastOnline"),
          PeriodFormat.between(lastJoin, System.currentTimeMillis())
              .retainBiggest()
      );
    }

    @Override
    public FieldPlacement placement() {
      return FieldPlacement.ALL;
    }
  };

  ProfileDisplayElement IP = (writer, user, context) -> {
    String ip = user.getIp();

    if (Strings.isNullOrEmpty(ip)) {
      return;
    }

    writer.field(
        MESSAGE_LIST.render("profile.ip"),
        text(ip)
            .hoverEvent(text("Click to copy"))
            .clickEvent(ClickEvent.copyToClipboard(ip))
    );
  };

  ProfileDisplayElement RETURN_LOCATION = (writer, user, context) -> {
    Location returnLoc = user.getReturnLocation();

    if (returnLoc == null) {
      return;
    }

    writer.formattedField("/back location", "{0, location, -c}", returnLoc);
  };

  ProfileDisplayElement LOCATION = (writer, user, context) -> {
    Location location = user.getLocation();
    if (location == null) {
      return;
    }

    writer.formattedField("Location", "{0, location, -c}", location);
  };

  ProfileDisplayElement SEPARATED_USERS = blockedUsers(UserBlockList::getSeparated, "separated");

  ProfileDisplayElement IGNORED_USERS = blockedUsers(UserBlockList::getBlocked, "blocked");

  static void registerAll(UserNameFactory factory) {
    factory.addProfileField("name", 0, REAL_NAME);
    factory.addProfileField("privacy_state", 10, PROFILE_PRIVATE_STATE);
    factory.addProfileField("last_online", 20, LAST_ONLINE);
    factory.addProfileField("first_join", 30, FIRST_JOIN);

    factory.addProfileField("balance", 40, BALANCE);
    factory.addProfileField("playtime", 60, PLAYTIME);
    factory.addProfileField("votes", 70, VOTES);

    factory.addProfileField("uuid", Integer.MAX_VALUE, UUID);

    factory.addAdminProfileField("ip", 10, IP);
    factory.addAdminProfileField("return_location", 20, RETURN_LOCATION);
    factory.addAdminProfileField("location", 30, LOCATION);
    factory.addAdminProfileField("separated", 40, SEPARATED_USERS);
    factory.addAdminProfileField("blocked", 50, IGNORED_USERS);
  }

  static ProfileDisplayElement blockedUsers(
      Function<UserBlockList, Collection<java.util.UUID>> getter,
      String name
  ) {
    MessageRef keyRef = MESSAGE_LIST.reference("profile." + name);
    MessageRef formatRef = MESSAGE_LIST.reference("profile." + name + ".format");

    return new ProfileDisplayElement() {
      @Override
      public void write(TextWriter writer, User user, DisplayContext context) {
        UserBlockList blockList = user.getComponent(UserBlockList.class);
        var separated = getter.apply(blockList);

        if (separated.isEmpty()) {
          return;
        }

        var blocked = CoreMessages.joinIds(separated, formatRef, context.viewer());

        writer.field(
            keyRef.get(),

            MESSAGE_LIST.render("profile.playerListButton")
                .create(context.viewer())
                .hoverEvent(blocked)
        );
      }

      @Override
      public FieldPlacement placement() {
        return FieldPlacement.IN_PROFILE;
      }
    };
  }

  static ProfileDisplayElement timestamp(String messageKey, TimeField field) {
    return (writer, user, context) -> {
      long value = user.getTime(field);

      if (value == -1) {
        return;
      }

      writer.formattedField(MESSAGE_LIST.render(messageKey), "{0, date}", value);
    };
  }
}
