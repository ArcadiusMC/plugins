package net.arcadiusmc.dialogues;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dialogues.commands.DialogueArgument;
import net.arcadiusmc.dialogues.commands.DialogueArgument.Result;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter
public class DialogueAction implements Action {

  private static final Logger LOGGER = Loggers.getLogger();

  static final ObjectType<DialogueAction> TYPE = new DialogueActionType();

  private final String dialogueName;
  private final String nodeName;

  public DialogueAction(String dialogueName, String nodeName) {
    this.dialogueName = dialogueName;
    this.nodeName = Strings.nullToEmpty(nodeName);
  }

  @Override
  public void onUse(Interaction interaction) {
    DialogueManager manager = DialoguesPlugin.plugin().getManager();
    Dialogue dialogue = manager.getRegistry().orNull(dialogueName);
    DialogueNode node;

    if (dialogue == null) {
      LOGGER.error("Unknown dialogue in usable {}: {}", interaction.getObject(), dialogueName);
      return;
    }

    if (Strings.isNullOrEmpty(nodeName)) {
      node = dialogue.getEntryPoint();

      if (node == null) {
        LOGGER.error("No entry-point in dialogue '{}'", dialogueName);
        return;
      }
    } else {
      node = dialogue.getNodeByName(nodeName);

      if (node == null) {
        LOGGER.error("Unknown node '{}' in dialogue '{}'", nodeName, dialogueName);
        return;
      }
    }

    long interactionId = manager.genInteractionId(interaction);
    node.use(interaction);
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(Dialogue.nodeIdentifier(dialogueName, nodeName));
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}

class DialogueActionType implements ObjectType<DialogueAction> {

  final Codec<DialogueAction> codec = Codec.STRING
      .comapFlatMap(
          s -> {
            if (s.contains(";")) {
              String[] split = s.split(";");
              if (split.length != 2) {
                return Results.error("Invalid dialogue identifier: '%s'", s);
              }
              return Results.success(new DialogueAction(split[0], split[1]));
            }

            return Results.success(new DialogueAction(s, ""));
          },
          dialogueAction -> {
            String dialogue = dialogueAction.getDialogueName();
            String node = dialogueAction.getNodeName();
            return Dialogue.nodeIdentifier(dialogue, node);
          }
      );

  @Override
  public DialogueAction parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    Result result = DialogueArgument.dialogue().parse(reader);
    String dialogueName = result.dialogue();
    String nodeName = result.nodeName();
    return new DialogueAction(dialogueName, nodeName);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    return DialogueArgument.dialogue().listSuggestions(context, builder);
  }

  @Override
  public <S> DataResult<DialogueAction> load(Dynamic<S> dynamic) {
    return codec.parse(dynamic);
  }

  @Override
  public <S> DataResult<S> save(@NotNull DialogueAction value, @NotNull DynamicOps<S> ops) {
    return codec.encodeStart(ops, value);
  }
}
