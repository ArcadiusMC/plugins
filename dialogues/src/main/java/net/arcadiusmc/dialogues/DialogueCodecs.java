package net.arcadiusmc.dialogues;

import static net.arcadiusmc.utils.io.ExtraCodecs.strictOptional;
import static net.kyori.adventure.text.Component.empty;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.dialogues.Link.DialogueLink;
import net.arcadiusmc.dialogues.Link.NodeLink;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.expr.ExprListCodec;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;

public class DialogueCodecs {

  static final Codec<Link> LINK_CODEC;

  static {
    MapCodec<NodeLink> nodeMapCodec = ExtraCodecs.KEY_CODEC
        .xmap(NodeLink::new, NodeLink::getNodeName)
        .fieldOf("node");

    MapCodec<DialogueLink> dialogueMapCodec = Codec.STRING
        .comapFlatMap(
            s -> ExtraCodecs.safeParse(s, reader -> {
              String dialogueName = Arguments.RESOURCE_KEY.parse(reader);
              String nodeName;

              reader.skipWhitespace();

              if (reader.canRead() && reader.peek() == ';') {
                reader.skip();
                nodeName = Arguments.RESOURCE_KEY.parse(reader);
              } else {
                nodeName = "";
              }

              return new DialogueLink(dialogueName, nodeName);
            }),
            o -> Dialogue.nodeIdentifier(o.dialogueName, o.nodeName)
        )
        .fieldOf("dialogue");

    MapCodec<Either<NodeLink, DialogueLink>> either
        = Codec.mapEither(nodeMapCodec, dialogueMapCodec);

    Codec<Link> linkCodec = either.codec()
        .xmap(
            e -> e.map(Function.identity(), Function.identity()),
            link -> {
              if (link instanceof NodeLink node) {
                return Either.left(node);
              } else {
                return Either.right((DialogueLink) link);
              }
            }
        );

    ExistingObjectCodec<Link> objectCodec = ExistingObjectCodec.create(builder -> {
      builder.optional("prompt", ExtraCodecs.COMPONENT)
          .getter(Link::getPromptOverride)
          .setter(Link::setPromptOverride);

      builder.optional("button-type", ExtraCodecs.enumCodec(ButtonType.class))
          .setter(Link::setButtonType)
          .getter(Link::getButtonType);

      builder.optional("prompt-hover", ExtraCodecs.COMPONENT)
          .getter(Link::getPromptHover)
          .setter(Link::setPromptHover);
    });

    LINK_CODEC = objectCodec.codec(linkCodec);
  }

  static final Codec<DialogueNode> NODE = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            strictOptional(ExtraCodecs.COMPONENT, "prompt", empty())
                .forGetter(DialogueNode::getPrompt),

            strictOptional(ExtraCodecs.COMPONENT, "prompt-hover", empty())
                .forGetter(DialogueNode::getPromptHover),

            strictOptional(ExtraCodecs.listOrValue(ExtraCodecs.COMPONENT), "text", List.of())
                .forGetter(DialogueNode::getContent),

            strictOptional(LINK_CODEC.listOf(), "links", List.of())
                .forGetter(DialogueNode::getLinks),

            strictOptional(ExprListCodec.ACTIONS_STRING_LIST, "on-view")
                .forGetter(o -> Optional.of(o.getExprList().getActions())),

            strictOptional(ExprListCodec.CONDITION_STRING_LIST, "conditions")
                .forGetter(o -> Optional.of(o.getExprList().getConditions())),

            strictOptional(DialogueOptions.CODEC, "settings")
                .forGetter(node -> Optional.ofNullable(node.getOptions())),

            strictOptional(Codec.BOOL, "invalidate-interaction", false)
                .forGetter(DialogueNode::isInvalidateInteraction)
        )
        .apply(instance, (prompt, hover, content, links, actions, conditions, options, invalidate) -> {
          DialogueNode node = new DialogueNode();
          node.setPrompt(prompt);
          node.setPromptHover(hover);
          node.getLinks().addAll(links);
          node.getContent().addAll(content);
          node.setInvalidateInteraction(invalidate);

          options.ifPresent(node::setOptions);

          actions.ifPresent(actions1 -> {
            for (Action action : actions1) {
              node.getExprList().getActions().addLast(action);
            }
          });

          conditions.ifPresent(conditions1 -> {
            for (Condition condition : conditions1) {
              node.getExprList().getConditions().addLast(condition);
            }
          });

          return node;
        });
  });

  static final Codec<Map<String, DialogueNode>> NODEMAP
      = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, NODE);
}
