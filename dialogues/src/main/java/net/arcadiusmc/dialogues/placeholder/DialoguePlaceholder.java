package net.arcadiusmc.dialogues.placeholder;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.dialogues.DialogueNode;
import net.arcadiusmc.dialogues.commands.DialogueArgument;
import net.arcadiusmc.dialogues.commands.DialogueArgument.Result;
import net.arcadiusmc.text.placeholder.ParsedPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.usables.Interaction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class DialoguePlaceholder implements ParsedPlaceholder {

  private final Interaction interaction;

  public DialoguePlaceholder(Interaction interaction) {
    this.interaction = interaction;
  }

  @Override
  public @Nullable Component render(StringReader reader, PlaceholderContext context)
      throws CommandSyntaxException
  {
    Result result = DialogueArgument.dialogue().parse(reader);
    DialogueNode node = result.node();

    return node.button(interaction, null, null, null);
  }
}
