package net.arcadiusmc.dialogues.placeholder;

import com.google.common.base.Strings;
import net.arcadiusmc.dialogues.Dialogue;
import net.arcadiusmc.dialogues.DialogueNode;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.arcadiusmc.usables.Interaction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class NodePlaceholder implements TextPlaceholder {

  private final Dialogue dialogue;
  private final Interaction interaction;

  public NodePlaceholder(Dialogue dialogue, Interaction interaction) {
    this.dialogue = dialogue;
    this.interaction = interaction;
  }

  @Override
  public @Nullable Component render(String match, PlaceholderContext render) {
    if (Strings.isNullOrEmpty(match)) {
      return null;
    }

    DialogueNode node = dialogue.getNodeByName(match);
    if (node == null) {
      return null;
    }

    return node.button(interaction, null, null, null);
  }
}
