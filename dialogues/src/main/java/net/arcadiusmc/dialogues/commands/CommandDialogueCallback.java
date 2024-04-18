package net.arcadiusmc.dialogues.commands;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.dialogues.Dialogue;
import net.arcadiusmc.dialogues.DialogueManager;
import net.arcadiusmc.dialogues.DialogueNode;
import net.arcadiusmc.usables.Interaction;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandDialogueCallback extends BaseCommand {

  private final DialogueManager manager;

  public CommandDialogueCallback(DialogueManager manager) {
    super("dialogue-callback");
    this.manager = manager;
    setDescription("Shhhhh...");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("d", LongArgumentType.longArg())
            .then(argument("n", LongArgumentType.longArg())
                .then(argument("i", LongArgumentType.longArg())
                    .executes(c -> {
                      long dialogueId = c.getArgument("d", Long.class);
                      long nodeId = c.getArgument("n", Long.class);
                      long interactionId = c.getArgument("i", Long.class);

                      DialogueNode node = null;

                      outer: for (Dialogue dialogue : manager.getRegistry()) {
                        if (dialogue.getRandomId() != dialogueId) {
                          continue;
                        }

                        for (DialogueNode dialogueNode : dialogue.getNodes()) {
                          if (dialogueNode.getRandomId() != nodeId) {
                            continue;
                          }

                          node = dialogueNode;
                          break outer;
                        }
                      }

                      if (node == null) {
                        return 0;
                      }

                      Interaction interaction = manager.getInteractionIdMap().get(interactionId);
                      if (interaction == null) {
                        return 0;
                      }

                      node.use(interaction);
                      return 0;
                    })
                )
            )
        );
  }
}
