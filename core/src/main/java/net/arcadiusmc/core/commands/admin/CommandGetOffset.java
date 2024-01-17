package net.arcadiusmc.core.commands.admin;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.entity.Player;
import org.spongepowered.math.vector.Vector3i;

public class CommandGetOffset extends BaseCommand {

  public CommandGetOffset() {
    super("GetOffset");

    setDescription("Gets the offset between 2 selected points");
    simpleUsages();
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /GetOffset
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          Player player = c.getSource().asPlayer();
          WorldBounds3i region = WorldBounds3i.ofPlayerSelection(player);

          if (region == null) {
            throw Exceptions.NO_REGION_SELECTION;
          }

          Vector3i dif = region.size();
          Vector3i dimensions = region.dimensions();

          player.sendMessage("dimensions: " + dimensions);
          player.sendMessage("dif: " + dif);
          player.sendMessage("distance: " + dif.length());

          return 0;
        });
  }
}