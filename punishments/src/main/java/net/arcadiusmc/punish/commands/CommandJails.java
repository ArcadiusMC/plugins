package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.arcadiusmc.WorldEditHook;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.punish.JailCell;
import net.arcadiusmc.punish.JailManager;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CommandJails extends BaseCommand {

  final JailManager manager;

  public CommandJails(JailManager manager) {
    super("jails");

    this.manager = manager;

    setDescription("Jail management plugin");
    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("create")
            .then(argument("name", Arguments.RESOURCE_KEY)
                .executes(c -> {
                  String name = c.getArgument("name", String.class);
                  return create(c.getSource(), name);
                })
            )
        );
  }

  private int create(CommandSource source, String name) throws CommandSyntaxException {
    Optional<JailCell> opt = manager.getCells().get(name);

    if (opt.isPresent()) {
      throw Messages.render("jails.alreadyExists")
          .addValue("name", name)
          .exception(source);
    }

    Player player = source.asPlayer();
    Location location = player.getLocation();

    WorldBounds3i selection = WorldEditHook.getPlayerSelection(player);

    if (selection == null) {
      throw Exceptions.NO_REGION_SELECTION;
    }

    World world = selection.getWorld();
    Bounds3i cellBounds = Bounds3i.of(selection);

    JailCell cell = new JailCell(cellBounds, Vectors.doubleFrom(location), world);
    manager.getCells().register(name, cell);

    source.sendSuccess(
        Messages.render("jails.created")
            .addValue("name", name)
            .create(source)
    );
    return 0;
  }
}
