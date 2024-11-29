package net.arcadiusmc.dialogues;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.expr.ExprList;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

@Getter
public class DialogueNode {

  private static final Logger LOGGER = Loggers.getLogger();

  private DialogueContent content;

  private final List<DialogueContent> potentialContents = new ObjectArrayList<>();

  @Setter
  private DialogueOptions options = DialogueOptions.defaultOptions();

  @Setter
  private Component prompt = Component.empty();

  @Setter
  private Component promptHover = Component.empty();

  @Setter
  private boolean invalidateInteraction;

  @Setter
  private boolean hideIfUnavailable;

  Dialogue entry;
  String key;

  @Setter
  long randomId;

  public DialogueNode() {

  }

  private ClickEvent getClickEvent(long interactionId) {
    return ClickEvent.runCommand(
        "/dialogue-callback "
            + entry.getRandomId()
            + " " + randomId
            + " " + interactionId
    );
  }

  public Component button(
      Interaction interaction,
      ButtonType type,
      Component promptOverride,
      Component hoverOverride
  ) {
    if (entry == null) {
      return null;
    }

    Audience viewer = interaction.getUser().orElse(null);
    Component baseText;
    DialogueRenderer renderer = new DialogueRenderer(interaction, this);

    Component prompt;
    Component promptHover;

    if (Text.isEmpty(promptOverride)) {
      prompt = this.prompt;
    } else {
      prompt = promptOverride;
    }

    if (Text.isEmpty(hoverOverride)) {
      promptHover = this.promptHover;
    } else {
      promptHover = hoverOverride;
    }

    if (Text.isEmpty(prompt)) {
      if (Strings.isNullOrEmpty(key)) {
        return null;
      }

      baseText = Component.text(key);
    } else {
      baseText = prompt;
    }

    ButtonType endType;
    Component hoverText;

    var exprList = content.getExprList();
    int failedIndex = exprList.getFailureIndex(interaction);

    if (failedIndex != NO_FAILURE) {
      if (hideIfUnavailable) {
        return null;
      }

      endType = ButtonType.UNAVAILABLE;
      hoverText = exprList.formatError(failedIndex, viewer, interaction);
    } else {
      endType = type == null ? ButtonType.REGULAR : type;

      if (Text.isEmpty(promptHover)) {
        hoverText = null;
      } else {
        hoverText = Placeholders.render(promptHover, viewer);
      }
    }

    Component buttonText = renderer.render(baseText);
    renderer.getPlaceholders().add("buttonText", buttonText);

    Component result = endType.render(renderer, hoverText);
    long interactionId = renderer.getInteraction().getValue("__id", Long.class).orElse(0L);

    return result.clickEvent(getClickEvent(interactionId));
  }

  public void use(Interaction interaction) {
    for (DialogueContent potentialContent : potentialContents) {
      if (!potentialContent.test(interaction)) {
        continue;
      }

      potentialContent.use(interaction);
      return;
    }

    content.use(interaction);
  }

  public void setContent(DialogueContent content) {
    this.content = content;

    if (content != null) {
      content.setNode(this);
    }
  }

  public void addPotentialContents(Collection<DialogueContent> list) {
    potentialContents.addAll(list);
    for (DialogueContent dialogueContent : list) {
      dialogueContent.setNode(this);
    }
  }
}