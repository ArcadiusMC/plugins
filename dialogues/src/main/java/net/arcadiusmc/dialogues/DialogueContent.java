package net.arcadiusmc.dialogues;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.expr.ExprList;
import net.arcadiusmc.usables.expr.ExprListCodec;
import net.arcadiusmc.usables.list.ActionsList;
import net.arcadiusmc.usables.list.ConditionsList;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Getter
public class DialogueContent {

  public static final ExistingObjectCodec<DialogueContent> E_CODEC
      = ExistingObjectCodec.create(builder -> {
        builder.optional("links", DialogueCodecs.LINK_CODEC.listOf())
            .getter(DialogueContent::getLinks)
            .setter((content, links) -> {
              content.getLinks().clear();
              content.getLinks().addAll(links);
            });

        builder.optional("conditions", ExprListCodec.CONDITION_STRING_LIST)
            .getter(content -> content.getExprList().getConditions())
            .setter((content, conditions) -> {
              ConditionsList out = content.getExprList().getConditions();

              for (int i = 0; i < conditions.size(); i++) {
                out.addLast(conditions.get(i));
                out.setError(i, conditions.getError(i));
              }
            });

        builder.optional("on-view", ExprListCodec.ACTIONS_STRING_LIST)
            .getter((content) -> content.getExprList().getActions())
            .setter((content, actions) -> {
              ActionsList out = content.getExprList().getActions();

              for (int i = 0; i < actions.size(); i++) {
                out.addLast(actions.get(i));
              }
            });

        builder.optional("text", ExtraCodecs.listOrValue(ExtraCodecs.COMPONENT))
            .getter(DialogueContent::getContent)
            .setter((content, list) -> {
              content.getContent().clear();
              content.getContent().addAll(list);
            });
      });

  public static final Codec<DialogueContent> CODEC
      = E_CODEC.codec(Codec.unit(DialogueContent::new));

  public static final MapCodec<DialogueContent> MAP_CODEC
      = E_CODEC.mapCodec(DialogueContent::new);

  private final ExprList exprList = new ExprList();
  private final List<Component> content = new ArrayList<>();
  private final List<Link> links = new ArrayList<>();

  @Setter
  private DialogueNode node;

  public DialogueContent() {
    exprList.setSilent(true);
  }

  public boolean test(Interaction interaction) {
    return exprList.test(interaction);
  }

  public void use(Interaction interaction) {
    int failureIndex = exprList.getFailureIndex(interaction);
    if (failureIndex != NO_FAILURE) {
      Player player = interaction.getPlayer().orElse(null);

      if (player == null) {
        return;
      }

      Component error = exprList.formatError(failureIndex, player, interaction);

      if (!Text.isEmpty(error)) {
        DialogueRenderer renderer = new DialogueRenderer(interaction, node);
        Component rendered = render(renderer, List.of(error));
        sendMessage(player, rendered);
      }

      return;
    }

    for (Condition condition : exprList.getConditions()) {
      condition.afterTests(interaction);
    }

    exprList.runActions(interaction);
    view(interaction);

    if (node.isInvalidateInteraction()) {
      long id = interaction.getValue("__id", Long.class).orElse(0L);

      if (id == 0)  {
        return;
      }

      DialogueManager manager = DialoguesPlugin.plugin().getManager();
      manager.getInteractionIdMap().remove(id);
      manager.freeId(id);
      interaction.getContext().remove("__id");
    }
  }

  private void view(Interaction interaction) {
    Optional<User> playerOpt = interaction.getUser();

    if (content.isEmpty() || playerOpt.isEmpty()) {
      return;
    }

    User user = playerOpt.get();

    DialogueRenderer renderer = new DialogueRenderer(interaction, node);
    Component text = render(renderer, content);

    sendMessage(user, text);
  }

  void sendMessage(Audience audience, Component message) {
    audience.sendMessage(message);

    DialogueOptions options = node.getOptions();
    if (options.getTalkSound() != null) {
      audience.playSound(options.getTalkSound());
    }
  }

  private Component render(DialogueRenderer renderer, List<Component> content) {
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
