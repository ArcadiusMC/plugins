package net.arcadiusmc.core.commands.admin;

import com.google.gson.JsonElement;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.utils.io.JsonUtils;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.nbt.BinaryTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;

public class CommandTextToJson extends BaseCommand {

  static final TextReplacementConfig NEWLINES = TextReplacementConfig.builder()
      .matchLiteral("\\n")
      .replacement("\n")
      .build();

  public CommandTextToJson() {
    super("texttojson");
    setAliases("tojson", "text-to-json");
    setDescription("Converts text into a JSON component");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("text", Arguments.CHAT)
        .executes(c -> {
          CommandSource source = c.getSource();

          ViewerAwareMessage message = Arguments.getMessage(c, "text");
          Component text = Placeholders.render(message.create(null)).replaceText(NEWLINES);

          JsonElement element = JsonUtils.writeText(text);

          String jsonString = element.toString();
          String nbtString = BinaryTags.stringTag(jsonString).toNbtString();

          Component jsonButton = Messages.renderText("cmd.texttojson.jsonString", source)
              .clickEvent(ClickEvent.copyToClipboard(jsonString))
              .hoverEvent(Component.text(jsonString));

          Component nbtButton = Messages.renderText("cmd.texttojson.nbtString", source)
              .clickEvent(ClickEvent.copyToClipboard(nbtString))
              .hoverEvent(Component.text(nbtString));

          source.sendMessage(
              Messages.render("cmd.texttojson.format")
                  .addValue("jsonString", jsonButton)
                  .addValue("nbtString", nbtButton)
                  .create(source)
          );

          return 0;
        })
    );
  }
}
