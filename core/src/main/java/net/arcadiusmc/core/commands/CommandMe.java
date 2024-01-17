package net.arcadiusmc.core.commands;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.utils.Audiences;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandMe extends BaseCommand {

  public CommandMe() {
    super("ftc_me");

    setAliases("me");
    setDescription("Command that everyone uses to make people think they died");
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /me <action>
   *
   * Permissions used:
   * ftc.default
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<action>")
        .addInfo("Broadcasts the <action> in chat")
        .addInfo("Lets you trick people into thinking you died")
        .addInfo("by doing '/me was blown up by Creeper'");
  }

  @Override
  public String getHelpListName() {
    return "me";
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("action", Arguments.MESSAGE)
            .executes(c -> {
              CommandSource source = c.getSource();
              ViewerAwareMessage message = Arguments.getMessage(c, "action");

              ChannelledMessage channelled = ChannelledMessage.create(message)
                  .setSource(source)
                  .setChannelName("commands/me")
                  .setBroadcast();

              channelled.setRenderer((viewer, baseMessage) -> {
                var builder = text();

                if (!Audiences.equals(viewer, source)) {
                  builder.append(text("* "), Text.sourceDisplayName(source, viewer), space());
                }

                return builder.append(baseMessage).build();
              });

              channelled.send();
              return 0;
            })
        );

    /*
    command
        .then(argument("action", StringArgumentType.greedyString())
            .executes(c -> {
              CommandSource source = c.getSource();
              boolean mayBroadcast = true;


              Component displayName = Text.sourceDisplayName(source);
              Component action = Text.renderString(
                  source.asBukkit(), c.getArgument("action", String.class)
              );

              //Check they didn't use a banned word
              if (BannedWords.checkAndWarn(source.asBukkit(), action)) {
                return 0;
              }

              Component formatted = Component.text()
                  .append(Component.text("* "))
                  .append(displayName)
                  .append(Component.space())
                  .append(action)
                  .build();

              source.sendMessage(formatted);

              if (mayBroadcast) {
                Users.getOnline()
                    .stream()
                    .filter(user -> !user.getName().equals(source.textName()))
                    .forEach(user -> user.sendMessage(formatted));
              }
              return 0;
            })
        );*/
  }
}