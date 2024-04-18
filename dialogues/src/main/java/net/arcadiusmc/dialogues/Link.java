package net.arcadiusmc.dialogues;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

public abstract class Link {

  @Setter @Getter
  protected Component promptOverride;

  @Getter @Setter
  protected ButtonType buttonType;

  @Getter @Setter
  protected Component promptHover;

  protected final Component render(DialogueNode node, DialogueRenderer renderer) {
    if (node == null) {
      return null;
    }
    return node.button(renderer.getInteraction(), buttonType, promptOverride, promptHover);
  }

  public abstract Component renderButton(DialogueRenderer renderer);

  static class NodeLink extends Link {

    @Getter
    final String nodeName;

    public NodeLink(String nodeName) {
      this.nodeName = nodeName;
    }

    @Override
    public Component renderButton(DialogueRenderer renderer) {
      DialogueNode node = renderer.getNode().getEntry().getNodeByName(nodeName);
      return render(node, renderer);
    }
  }

  static class DialogueLink extends Link {

    final String dialogueName;
    final String nodeName;

    public DialogueLink(String dialogueName, String nodeName) {
      this.dialogueName = dialogueName;
      this.nodeName = nodeName;
    }

    @Override
    public Component renderButton(DialogueRenderer renderer) {
      DialogueManager manager = DialoguesPlugin.plugin().getManager();
      Dialogue dialogue = manager.getRegistry().orNull(dialogueName);

      if (dialogue == null) {
        return null;
      }

      DialogueNode node;

      if (Strings.isNullOrEmpty(nodeName)) {
        node = dialogue.getEntryPoint();
      } else {
        node = dialogue.getNodeByName(nodeName);
      }

      return render(node, renderer);
    }
  }
}
