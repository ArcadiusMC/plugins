package net.arcadiusmc.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.forthecrown.grenadier.GrenadierCommand;

public class TabPrefixSuffixCommand extends BaseCommand {

  private final CorePlugin plugin;

  private final UserProperty<String> property;

  private final String clearKey;
  private final String setKey;
  private final String lengthErrorKey;

  public TabPrefixSuffixCommand(
      CorePlugin plugin,
      String name,
      UserProperty<String> property,
      String messageKey
  ) {
    super(name);

    this.plugin = plugin;
    this.property = property;

    this.clearKey = "cmd." + messageKey + ".cleared";
    this.setKey = "cmd." + messageKey + ".set";
    this.lengthErrorKey = "cmd." + messageKey + ".errors.maxLength";
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("clear")
            .executes(c -> {
              User user = getUserSender(c);
              user.set(property, null);

              user.sendMessage(Messages.renderText(clearKey, user));
              return SINGLE_SUCCESS;
            })
        )

        .then(argument("value", StringArgumentType.greedyString())
            .executes(c -> {
              User user = getUserSender(c);
              String value = StringArgumentType.getString(c, "value");

              if (value.length() >= plugin.getCoreConfig().maxSuffixLength()) {
                throw Messages.render(lengthErrorKey)
                    .addValue("maxLength", plugin.getCoreConfig().maxSuffixLength())
                    .addValue("value", value)
                    .addValue("length", value.length())
                    .exception(user);
              }

              user.set(property, value);

              user.sendMessage(
                  Messages.render(setKey)
                      .addValue("value", value)
                      .create(user)
              );

              return SINGLE_SUCCESS;
            })
        );
  }
}
