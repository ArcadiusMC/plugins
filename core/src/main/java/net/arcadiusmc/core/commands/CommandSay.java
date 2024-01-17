package net.arcadiusmc.core.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.entity.Player;

public class CommandSay extends BaseCommand {

  public CommandSay() {
    super("Say");

    setDescription("Says a message in chat");
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /Say
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("msg", StringArgumentType.greedyString())
            .executes(c -> {
              String msg = c.getArgument("msg", String.class);

              if (c.getSource().isPlayer()) {
                Player player = c.getSource().asPlayer();
                player.chat(msg);
              } else {
                ChannelledMessage.create(PlayerMessage.allFlags(msg))
                    .setBroadcast()
                    .setSource(c.getSource())
                    .setRenderer((viewer, baseMessage) -> {
                      return Messages.chatMessage(c.getSource().displayName(), baseMessage);
                    })
                    .send();
              }

              return 0;
            })
        );
  }
}