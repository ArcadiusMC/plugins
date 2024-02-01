package net.arcadiusmc.core.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Audiences;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;

public class CommandTell extends BaseCommand {

  static final MessageRef TELL_FORMAT = Messages.MESSAGE_LIST.reference("cmd.tell.format");
  static final MessageRef SELF = Messages.MESSAGE_LIST.reference("cmd.tell.self");

  public CommandTell() {
    super("ftell");

    setAliases(
        "emsg", "tell", "whisper",
        "w", "msg", "etell",
        "ewhisper", "pm", "dm",
        "t", "message"
    );

    setPermission(CorePermissions.MESSAGE);
    setDescription("Sends a message to a player");

    register();
  }

  @Override
  public String getHelpListName() {
    return "tell";
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player> <message>")
        .addInfo("Sends a <message> to <player>")
        .addInfo("Donators can use color codes and emotes");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        // /tell <user>
        .then(argument("user", Arguments.ONLINE_USER)

            // /tell <user> <message>
            .then(argument("message", Arguments.MESSAGE)

                .executes(c -> {
                  CommandSource source = c.getSource();
                  User user = Arguments.getUser(c, "user");
                  ViewerAwareMessage text = Arguments.getMessage(c, "message");

                  return send(source, user, text);
                })
            )
        );
  }

  public int send(CommandSource sender, User target, ViewerAwareMessage message)
      throws CommandSyntaxException
  {
    CommandSource receiver = target.getCommandSource();

    if (sender.isPlayer()) {
      User user = Users.get(sender.asPlayer());

      if (!user.equals(target)) {
        user.setLastMessage(receiver);
        target.setLastMessage(sender);
      }
    }

    target.setLastMessage(sender);

    run(sender, target.getCommandSource(), message);
    return 0;
  }

  static void run(CommandSource sender, CommandSource target, ViewerAwareMessage message) {
    ChannelledMessage channelled = ChannelledMessage.create(message)
        .setSource(sender)
        .addTarget(target)
        .setChannelName("commands/tell");

    channelled.setRenderer((viewer, baseMessage) -> {
      Component firstDisplay;
      Component secondDisplay;

      Component me = SELF.renderText(viewer);

      if (Audiences.equals(sender, target) && Audiences.equals(viewer, sender)) {
        firstDisplay = me;
        secondDisplay = me;
      } else if (Audiences.equals(viewer, sender)) {
        firstDisplay = me;
        secondDisplay = Text.sourceDisplayName(target, viewer);
      } else if (Audiences.equals(viewer, target)) {
        firstDisplay = Text.sourceDisplayName(sender, viewer);
        secondDisplay = me;
      } else {
        firstDisplay = Text.sourceDisplayName(sender, viewer);
        secondDisplay = Text.sourceDisplayName(target, viewer);
      }

      return TELL_FORMAT.get()
          .addValue("targetSource", target)
          .addValue("senderSource", target)

          .addValue("sender", firstDisplay)
          .addValue("target", secondDisplay)
          .addValue("message", baseMessage)

          .create(viewer);
    });

    channelled.send();
  }
}