package net.arcadiusmc.staffchat;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.command.ConsoleCommandSender;

public class CommandStaffChat extends BaseCommand {

  public CommandStaffChat() {
    super("staffchat");

    setPermission(StaffChat.PERMISSION);
    setAliases("sc");
    setDescription("Sends a message to the staff chat");

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   * Sends a message to the staff chat
   *
   *
   * Valid usages of command:
   * - /staffchat
   * - /sc
   *
   * Permissions used:
   * - ftc.staffchat
   *
   * Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<message>", "Sends a <message> to the staff chat");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("message", Arguments.CHAT)
        .executes(c -> {
          ViewerAwareMessage message = Arguments.getMessage(c, "message");

          StaffChat.newMessage()
              .setSource(c.getSource())
              .setMessage(message)
              .setLogged(!c.getSource().is(ConsoleCommandSender.class))
              .send();

          return 0;
        })
    );
  }
}