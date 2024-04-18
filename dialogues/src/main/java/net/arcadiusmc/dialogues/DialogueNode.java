package net.arcadiusmc.dialogues;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;

import com.google.common.base.Strings;
import java.util.ArrayList;
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
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.usables.expr.ExprList;
import net.arcadiusmc.user.User;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.slf4j.Logger;

@Getter
public class DialogueNode {

  private static final Logger LOGGER = Loggers.getLogger();

  private final List<Component> content = new ArrayList<>();

  private final List<Link> links = new ArrayList<>();

  private final ExprList exprList = new ExprList();

  @Setter
  private DialogueOptions options = DialogueOptions.defaultOptions();

  @Setter
  private Component prompt = Component.empty();

  @Setter
  private Component promptHover = Component.empty();

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
    int failedIndex = exprList.getFailureIndex(interaction);

    if (failedIndex != NO_FAILURE) {
      Condition failed = exprList.getConditions().get(failedIndex);
      String errorOverride = exprList.getConditions().getError(failedIndex);
      endType = ButtonType.UNAVAILABLE;

      if (Strings.isNullOrEmpty(errorOverride)) {
        hoverText = failed.failMessage(interaction);
      } else {
        hoverText = Usables.formatBaseString(errorOverride, viewer);
      }
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
    if (!exprList.interact(interaction)) {
      return;
    }

    view(interaction);
  }

  private void view(Interaction interaction) {
    Optional<User> playerOpt = interaction.getUser();

    if (content.isEmpty() || playerOpt.isEmpty()) {
      return;
    }

    User user = playerOpt.get();

    DialogueRenderer renderer = new DialogueRenderer(interaction, this);
    Component text = render(renderer);

    user.sendMessage(text);
  }

  public Component render(DialogueRenderer renderer) {
    TextJoiner joiner = TextJoiner.newJoiner();

    DialogueOptions options = renderer.getOptions();
    Component prefix = options.getPrefix();
    Component suffix = options.getSuffix();

    if (!Text.isEmpty(prefix)) {
      joiner.add(renderer.render(prefix));
    }

    TextJoiner nlJoiner = TextJoiner.onNewLine();
    nlJoiner.add(content.stream().map(renderer::render));

    for (Link link : links) {
      Component text = link.renderButton(renderer);
      if (text == null) {
        continue;
      }
      nlJoiner.add(text);
    }

    joiner.add(nlJoiner.asComponent());

    if (!Text.isEmpty(suffix)) {
      joiner.add(renderer.render(suffix));
    }

    return joiner.asComponent();
  }
}