package net.arcadiusmc.dialogues;

import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dialogues.placeholder.DialoguePlaceholder;
import net.arcadiusmc.dialogues.placeholder.NodePlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.usables.Interaction;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.EnumArgument;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Getter
public class DialogueRenderer {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final EnumArgument<ButtonType> BUTTON_PARSER
      = ArgumentTypes.enumType(ButtonType.class);


  private final PlaceholderRenderer placeholders;

  private final Interaction interaction;
  private final DialogueNode node;

  public DialogueRenderer(Interaction interaction, DialogueNode node) {
    this.interaction = interaction;
    this.node = node;

    this.placeholders = Placeholders.newRenderer()
        .useDefaults()
        .add("node", new NodePlaceholder(node.entry, interaction))
        .add("dialogue", new DialoguePlaceholder(interaction));
  }

  public DialogueOptions getOptions() {
    return node.getOptions();
  }

  public Component render(Component text) {
    return placeholders.render(text, interaction.getUser().orElse(null));
  }
}