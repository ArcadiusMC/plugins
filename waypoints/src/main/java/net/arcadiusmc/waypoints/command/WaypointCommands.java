package net.arcadiusmc.waypoints.command;

import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.WaypointProperties;
import net.arcadiusmc.waypoints.WaypointProperty;

public class WaypointCommands {

  public static final WaypointArgument WAYPOINT = new WaypointArgument();

  public static final RegistryArguments<WaypointProperty> PROPERTY
      = new RegistryArguments<>(WaypointProperties.REGISTRY, "Waypoint property");

  public static void createCommands(WaypointManager manager) {
    new CommandFindPole();
    new CommandHomeWaypoint();
    new CommandInvite();
    new CommandListWaypoints();
    new CommandMoveIn();
    new CommandSpawn();
    new CommandVisit();
    new CommandWaypointGui();

    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new CommandWaypoints(manager));
  }
}