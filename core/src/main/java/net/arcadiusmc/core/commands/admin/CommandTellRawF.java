package net.arcadiusmc.core.commands.admin;

import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.arguments.ExpandedEntityArgument;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Entity;

public class CommandTellRawF extends BaseCommand {

  static final TextReplacementConfig NL_REPLACER = TextReplacementConfig.builder()
      .matchLiteral("\\n")
      .replacement(Component.newline())
      .build();

  public CommandTellRawF() {
    super("tellrawf");
    setAliases("tellraw", "ftellraw");
    setDescription("ArcadiusMC's version of /tellraw with more lax text input");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("targets", new ExpandedEntityArgument(true, true))
            .then(argument("message", Arguments.CHAT)
                .executes(c -> {
                  ViewerAwareMessage message = Arguments.getMessage(c, "message");
                  List<Entity> entities = ArgumentTypes.getEntities(c, "targets");

                  PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();

                  for (Entity entity : entities) {
                    Component rendered = message.create(entity);
                    rendered = renderer.render(rendered, entity);
                    entity.sendMessage(replaceNewlines(rendered));
                  }

                  return 0;
                })
            )
        );
  }

  private static Component replaceNewlines(Component text) {
    return text.replaceText(NL_REPLACER);
  }
}
