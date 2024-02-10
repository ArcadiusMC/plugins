package net.arcadiusmc.core.commands.tools;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.user.UserServiceImpl;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.forthecrown.grenadier.CommandContexts;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.Readers;

public class CommandUserProperty extends BaseCommand {

  final UserServiceImpl service;

  public CommandUserProperty(CorePlugin plugin) {
    super("set-user-property");
    this.service = plugin.getUserService();

    setDescription("Sets the value of any user property");
    setPermission(Commands.getDefaultPermission("setuserproperty"));

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player> <property> <value>")
        .addInfo("Sets a user's user-property value");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    RegistryArguments<UserProperty<?>> propertyType
        = new RegistryArguments<>(service.getUserProperties(), "User Property");

    command
        .then(argument("player", Arguments.USER)
            .then(argument("property", propertyType)
                .then(argument("value", StringArgumentType.greedyString())
                    .suggests((c, builder) -> {
                      Holder<UserProperty<?>> propertyHolder
                          = c.getArgument("property", Holder.class);

                      return propertyHolder.getValue()
                          .getArgumentType()
                          .listSuggestions(c, builder);
                    })

                    .executes(c -> {
                      User user = Arguments.getUser(c, "player");

                      Holder<UserProperty<Object>> propertyHolder
                          = c.getArgument("property", Holder.class);

                      UserProperty<Object> property = propertyHolder.getValue();

                      StringReader reader = Readers.fromContextInput(c);
                      var range = CommandContexts.getNodeRange(c, "value");
                      reader.setCursor(range.getStart());

                      Object value = property.getArgumentType().parse(reader);
                      Commands.ensureCannotRead(reader);

                      user.set(property, value);

                      c.getSource().sendSuccess(
                          Messages.render("cmd.setuserproperty.set")
                              .addValue("property", propertyHolder.getKey())
                              .addValue("player", user)
                              .addValue("value", value)
                              .create(c.getSource())
                      );
                      return 0;
                    })
                )
            )
        );
  }
}
