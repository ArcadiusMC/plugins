package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.announcer.AutoAnnouncer;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;

public class CommandBroadcast extends BaseCommand {

  public CommandBroadcast() {
    super("broadcast");

    setDescription("Broadcasts a message to the entire server.");
    setAliases("announce", "bc", "ac");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<message>", "Broadcasts a <message> to the entire server");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("message", Arguments.CHAT)
            .executes(c -> {
              ViewerAwareMessage message = Arguments.getMessage(c, "message");
              PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();
              AutoAnnouncer announcer = CorePlugin.plugin().getAnnouncer();

              ChannelledMessage channelled = ChannelledMessage.create(message)
                  .setBroadcast()
                  .setRenderer(announcer.renderer(renderer))
                  .setSource(c.getSource());

              channelled.send();
              return 0;
            })
        );
  }
}