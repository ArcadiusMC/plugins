package net.arcadiusmc.core.commands.tools;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.jetbrains.annotations.Nullable;

public class CommandCloseInventory extends BaseCommand {

  public CommandCloseInventory() {
    super("close-inventory");

    setAliases("close-menu");
    setDescription("Closes a player's inventory");
    setPermission(Commands.getDefaultPermission("closeinv"));

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<player>")
        .addInfo("Closes a player's inventory");

    factory.usage("<player> <reason>")
        .addInfo("Closes a player's inventory for a specific reason");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command.then(argument("player", Arguments.ONLINE_USER)
        .executes(c -> closeInventory(c, null))

        .then(argument("reason", ArgumentTypes.enumType(Reason.class))
            .executes(c -> {
              Reason reason = c.getArgument("reason", Reason.class);
              return closeInventory(c, reason);
            })
        )
    );
  }

  private int closeInventory(CommandContext<CommandSource> context, @Nullable Reason reason)
      throws CommandSyntaxException
  {
    CommandSource source = context.getSource();

    User user = Arguments.getUser(context, "player");
    Player player = user.getPlayer();

    MessageRender message;

    if (source.isPlayer()) {
      if (reason == null) {
        player.closeInventory();
        message = Messages.render("cmd.closeinv.closed");
      } else {
        player.closeInventory(reason);
        message = Messages.render("cmd.closeinv.closed.reason")
            .addValue("reason", reason);
      }

      source.sendSuccess(
          message
              .addValue("player", player)
              .create(source)
      );
    }

    return 0;
  }
}
