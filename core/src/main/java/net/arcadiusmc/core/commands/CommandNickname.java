package net.arcadiusmc.core.commands;

import static net.arcadiusmc.core.CoreMessages.NICK_CLEARED_OTHER;
import static net.arcadiusmc.core.CoreMessages.NICK_CLEARED_SELF;
import static net.arcadiusmc.core.CoreMessages.NICK_NONE_SET;
import static net.arcadiusmc.core.CoreMessages.NICK_SET_OTHER;
import static net.arcadiusmc.core.CoreMessages.NICK_SET_SELF;
import static net.arcadiusmc.core.CoreMessages.NICK_TOO_LONG;
import static net.arcadiusmc.core.CoreMessages.NICK_UNAVAILABLE;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserLookup;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandNickname extends BaseCommand {

  public static final String CLEAR = "-clear";

  public CommandNickname() {
    super("nickname");

    setAliases("nick");
    setPermission(CorePermissions.CMD_NICKNAME);
    setDescription("Sets your nickname");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("", "Clears your nickname");
    factory.usage("<nick>", "Sets your nickname");

    factory.usage("<player> <nick>", "Sets a player's nickname");
    factory.usage("<player> clear", "Clears a player's nickname");
  }

  enum NickSetResult {
    SUCCESS,
    TAKEN,
    TOO_LONG,
    ALREADY_NICK,
    ;
  }

  NickSetResult trySet(User target, String newnick) {
    if (newnick.length() > CorePlugin.plugin().getCoreConfig().maxNickLength()) {
      return NickSetResult.TOO_LONG;
    }

    UserLookup lookup = Users.getService().getLookup();
    LookupEntry query = lookup.query(newnick);

    if (query != null) {
      if (query.getUniqueId().equals(target.getUniqueId())) {
        return NickSetResult.ALREADY_NICK;
      }

      return NickSetResult.TAKEN;
    }

    target.setNickname(newnick);
    return NickSetResult.SUCCESS;
  }

  void setSelf(User user, String nickname) throws CommandSyntaxException {
    NickSetResult result = trySet(user, nickname);
    switch (result) {
      case SUCCESS -> {
        user.sendMessage(
            NICK_SET_SELF.get()
                .addValue("nick", nickname)
                .create(user)
        );
      }

      case TOO_LONG -> {
        int maxLength = CorePlugin.plugin().getCoreConfig().maxNickLength();
        throw NICK_TOO_LONG.get()
            .addValue("maxLength", maxLength)
            .addValue("nick", nickname)
            .exception(user);
      }

      case TAKEN -> {
        throw NICK_UNAVAILABLE.get()
            .addValue("nick", nickname)
            .exception(user);
      }

      case ALREADY_NICK -> {
        throw Messages.render("cmd.nickname.error.alreadySet.self")
            .addValue("nick", nickname)
            .exception(user);
      }
    }
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          user.setNickname(null);
          user.sendMessage(NICK_CLEARED_SELF.renderText(user));
          return 0;
        })

        .then(argument("nick", StringArgumentType.word())
            .executes(c -> {
              User user = getUserSender(c);
              String nickname = c.getArgument("nick", String.class);

              setSelf(user, nickname);
              return 0;
            })
        )

        .then(argument("player", Arguments.USER)
            .requires(source -> source.hasPermission(getAdminPermission()))

            .then(literal("clear")
                .executes(c -> {
                  CommandSource source = c.getSource();
                  User target = Arguments.getUser(c, "player");

                  if (Strings.isNullOrEmpty(target.getNickname())) {
                    throw NICK_NONE_SET.get()
                        .addValue("player", target)
                        .exception(source);
                  }

                  target.setNickname(null);

                  source.sendSuccess(
                      NICK_CLEARED_OTHER.get()
                          .addValue("player", target)
                          .create(source)
                  );
                  return 0;
                })
            )

            .then(argument("nick", StringArgumentType.word())
                .executes(c -> {
                  CommandSource source = c.getSource();
                  User target = Arguments.getUser(c, "player");

                  String nick = c.getArgument("nick", String.class);
                  target.setNickname(nick);

                  boolean self = source.textName().equals(target.getName());

                  if (self) {
                    setSelf(target, nick);
                    return 0;
                  }

                  NickSetResult result = trySet(target, nick);
                  switch (result) {
                    case SUCCESS -> {
                      source.sendSuccess(
                          NICK_SET_OTHER.get()
                              .addValue("player", target)
                              .addValue("nick", nick)
                              .create(source)
                      );
                    }

                    case ALREADY_NICK -> {
                      throw Messages.render("cmd.nickname.error.alreadySet.other")
                          .addValue("nick", nick)
                          .exception(source);
                    }
                    case TOO_LONG -> {
                      int maxlen = CorePlugin.plugin().getCoreConfig().maxNickLength();

                      throw NICK_TOO_LONG.get()
                          .addValue("nick", nick)
                          .addValue("maxLength", maxlen)
                          .exception(source);
                    }
                    case TAKEN -> {
                      throw NICK_UNAVAILABLE.get()
                          .addValue("nick", nick)
                          .exception(source);
                    }
                  }

                  return 0;
                })
            )
        );
  }
}