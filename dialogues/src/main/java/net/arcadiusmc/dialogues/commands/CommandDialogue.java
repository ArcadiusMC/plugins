package net.arcadiusmc.dialogues.commands;

import com.mojang.brigadier.Command;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.ExpandedEntityArgument;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.dialogues.DialogueNode;
import net.arcadiusmc.dialogues.DialoguesPlugin;
import net.arcadiusmc.dialogues.commands.DialogueArgument.Result;
import net.arcadiusmc.text.Text;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class CommandDialogue extends BaseCommand {

  final DialoguesPlugin plugin;

  public CommandDialogue(DialoguesPlugin plugin) {
    super("dialogues");

    this.plugin = plugin;

    setDescription("Dialogues plugin command");
    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("reload")
        .addInfo("Reload dialogues and the plugin config");

    factory.usage("view <dialogue>[;<node>]")
        .addInfo("Views a dialogue or a node within a dialogue tree")
        .addInfo("Example usage:")
        .addInfo("/%s view abilityNpcConvo:node_dash", getName());

    factory.usage("view <dialogue>[;<node>] <players>")
        .addInfo("Shows player(s) a dialogue or a node within a dialogue tree")
        .addInfo("Example usages:")
        .addInfo("/%s view abilityNpcConvo:node_dash Julie", getName())
        .addInfo("/%s view abilityNpcConvo:node_dash @a[distance=..2]", getName());
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("reload")
            .executes(c -> {
              plugin.getManager().load();
              c.getSource().sendSuccess(Component.text("Reloaded dialogues plugin"));
              return 0;
            })
        )

        .then(literal("view")
            .then(argument("dialogue", DialogueArgument.dialogue())
                .executes(showDialogue(false))

                .then(argument("players", new ExpandedEntityArgument(true, true))
                    .executes(showDialogue(true))
                )
            )
        );
  }

  private Command<CommandSource> showDialogue(boolean targetsGiven) {
    return c -> {
      Result result = c.getArgument("dialogue", Result.class);
      DialogueNode node = result.node();

      List<Player> players;

      if (targetsGiven) {
        players = ArgumentTypes.getPlayers(c, "players");
      } else {
        var player = c.getSource().asPlayer();
        players = List.of(player);
      }

      players.forEach(node::accept);

      c.getSource().sendSuccess(
          Text.format("Showed a dialogue node to {0, number} players", players.size())
      );
      return 0;
    };
  }
}
