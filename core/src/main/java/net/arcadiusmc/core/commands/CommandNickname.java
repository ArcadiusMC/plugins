package net.arcadiusmc.core.commands;

import static net.arcadiusmc.core.CoreMessages.NICK_CLEARED_OTHER;
import static net.arcadiusmc.core.CoreMessages.NICK_CLEARED_SELF;
import static net.arcadiusmc.core.CoreMessages.NICK_NONE_SET;
import static net.arcadiusmc.core.CoreMessages.NICK_SET_OTHER;
import static net.arcadiusmc.core.CoreMessages.NICK_SET_SELF;
import static net.arcadiusmc.core.CoreMessages.NICK_TOO_LONG;
import static net.arcadiusmc.core.CoreMessages.NICK_UNAVAILABLE;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CoreConfig;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.core.CorePlugin;
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

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          User user = getUserSender(c);
          user.setNickname(null);
          user.sendMessage(NICK_CLEARED_SELF.renderText(user));
          return 0;
        })

        .then(argument("nick", new NicknameArgument())
            .executes(c -> {
              User user = getUserSender(c);
              String nickname = c.getArgument("nick", String.class);
              user.setNickname(nickname);

              user.sendMessage(
                  NICK_SET_SELF.get()
                      .addValue("nick", nickname)
                      .create(user)
              );

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

            .then(argument("nick", new NicknameArgument())
                .executes(c -> {
                  CommandSource source = c.getSource();
                  User target = Arguments.getUser(c, "player");

                  String nick = c.getArgument("nick", String.class);

                  target.setNickname(nick);

                  boolean self = source.textName().equals(target.getName());

                  if (self) {
                    source.sendMessage(
                        NICK_SET_SELF.get()
                            .addValue("nick", nick)
                            .create(target)
                    );
                  } else {
                    source.sendSuccess(
                        NICK_SET_OTHER.get()
                            .addValue("player", target)
                            .addValue("nick", nick)
                            .create(source)
                    );
                  }

                  return 0;
                })
            )
        );
  }

  private static class NicknameArgument implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
      String nick = reader.readUnquotedString();

      CoreConfig config = CorePlugin.plugin().getFtcConfig();
      int maxLength = config.maxNickLength();

      if (nick.length() > maxLength) {
        throw NICK_TOO_LONG.get()
            .addValue("maxLength", maxLength)
            .addValue("nick", nick)
            .exception();
      }

      UserLookup cache = Users.getService().getLookup();
      LookupEntry entry = cache.query(nick);

      if (entry != null) {
        throw NICK_UNAVAILABLE.get()
            .addValue("nick", nick)
            .exception();
      }

      return nick;
    }
  }
}