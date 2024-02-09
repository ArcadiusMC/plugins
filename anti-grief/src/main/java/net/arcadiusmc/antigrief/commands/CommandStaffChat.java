package net.arcadiusmc.antigrief.commands;

import net.arcadiusmc.antigrief.GriefPermissions;
import net.arcadiusmc.antigrief.StaffChat;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.text.ViewerAwareMessage;
import org.bukkit.command.ConsoleCommandSender;

public class CommandStaffChat extends FtcCommand {

  public CommandStaffChat() {
    super("sc");

    setPermission(GriefPermissions.STAFF_CHAT);
    setAliases("staffchat");
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