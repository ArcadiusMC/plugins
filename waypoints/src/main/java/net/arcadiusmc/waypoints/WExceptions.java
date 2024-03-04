package net.arcadiusmc.waypoints;

import static net.arcadiusmc.command.Exceptions.format;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.waypoints.type.WaypointType;
import net.arcadiusmc.waypoints.type.WaypointTypes;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.spongepowered.math.vector.Vector3i;

public interface WExceptions {

  static CommandSyntaxException noHomeRegion() {
    return Messages.render("waypoints.errors.noHome.self").exception();
  }

  static CommandSyntaxException onlyInVehicle() {
    return Messages.render("waypoints.errors.visitInVehicle").exception();
  }

  static CommandSyntaxException farFromWaypoint() {
    return Messages.render("waypoints.errors.farFromWaypoint").exception();
  }

  static CommandSyntaxException unloadedWorld() {
    return Messages.render("waypoints.errors.unloadedWorld").exception();
  }

  static CommandSyntaxException faceWaypoint() {
    return Messages.render("waypoints.errors.faceWaypoint").exception();
  }

  static CommandSyntaxException unknownRegion(StringReader reader, int cursor) {
    return Messages.render("waypoints.errors.unknown")
        .addValue("name", reader.getString().substring(cursor, reader.getCursor()))
        .exception();
  }

  static CommandSyntaxException farFromWaypoint(Waypoint waypoint) {
    return farFromWaypoint(waypoint.getPosition());
  }

  static CommandSyntaxException farFromWaypoint(Vector3i pos) {
    return Messages.render("waypoints.errors.farFromWaypoint.pos")
        .addValue("position", pos)
        .exception();
  }

  static CommandSyntaxException privateRegion(Waypoint region) {
    return Messages.render("waypoints.errors.privateRegion")
        .addValue("waypoint", region.getEffectiveName())
        .exception();
  }

  static CommandSyntaxException brokenWaypoint(Vector3i pos, Material found, Material expected) {
    return Messages.render("waypoints.errors.broken")
        .addValue("position", pos)
        .addValue("expected", expected)
        .addValue("found", found)
        .exception();
  }

  static CommandSyntaxException invalidWaypointTop(Material m) {
    Component tops = TextJoiner.onComma()
        .add(
            WaypointTypes.REGISTRY.stream()
                .map(Holder::getValue)
                .filter(WaypointType::isBuildable)
                .map(type -> {
                  Material[] col = type.getColumn();
                  Material top = col[col.length - 1];

                  return Text.format("{0} ({1} waypoint)",
                      top, type.getDisplayName().toLowerCase().replace("-made", "")
                  );
                })
        )
        .asComponent();

    return format("{0} is an invalid waypoint top block! Must be one of: {1}", m, tops);
  }

  static CommandSyntaxException waypointBlockNotEmpty(Block pos) {
    var areaSize = playerWaypointSize();

    return format(
        "Waypoint requires a clear {1}x{2}x{3} area around it!\n"
            + "Non-empty block found at {0, vector}",

        Vectors.from(pos),
        areaSize.x(), areaSize.y(), areaSize.z()
    );
  }

  static CommandSyntaxException snowTooHigh(Block block) {
    var areaSize = playerWaypointSize();
    var pos = Vectors.from(block);

    return format("Waypoint requires a clear {1}x{2}x{3} area around it!\n" +
        "Snow block higher than half a block found at {0, vector}",
        pos,
        areaSize.x(), areaSize.y(), areaSize.z()
    );
  }

  static CommandSyntaxException overlappingWaypoints(int overlapping) {
    return format("This waypoint is overlapping {0, number} other waypoint(s)", overlapping);
  }

  static CommandSyntaxException waypointPlatform() {
    var size = playerWaypointSize();
    return waypointPlatform(size);
  }

  static CommandSyntaxException waypointPlatform(Vector3i size) {
    return format("Waypoint requires a {0}x{1} platform under it!", size.x(), size.z());
  }

  private static Vector3i playerWaypointSize() {
    return WaypointManager.getInstance().config().playerWaypointSize;
  }


  static CommandSyntaxException notInvited(Audience viewer, User user) {
    return Messages.render("waypoints.errors.notInvited")
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException noHomeWaypoint(Audience viewer, User user) {
    return Messages.render("waypoints.errors.noHome.other")
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException nonReplaceableFloorBlock(Block block) {
    Vector3i vec = Vectors.from(block);

    return Messages.render("waypoints.errors.nonReplaceable")
        .addValue("position", vec)
        .addValue("block", block.getType())
        .exception();
  }

  static CommandSyntaxException waypointAlreadySet(Waypoint existing) {
    Vector3i p = existing.getPosition();
    Location location = new Location(existing.getWorld(), p.x(), p.y(), p.z());

    return Messages.render("waypoints.errors.alreadySet")
        .addValue("existing", location)
        .addValue("how",
            Messages.render("waypoints.errors.alreadySet.how")
                .asComponent()
                .hoverEvent(Messages.renderText("waypoints.errors.alreadySet.how.hover", null))
        )
        .exception();
  }

  static CommandSyntaxException creationDisabled() {
    return Messages.render("waypoints.errors.creationDisabled").exception();
  }

  static CommandSyntaxException waypointNotDiscovered(Waypoint waypoint) {
    Component displayName = waypoint.displayName();

    if (displayName == null) {
      return Messages.render("waypoints.errors.notDiscovered.unnamed").exception();
    }

    return Messages.render("waypoints.errors.notDiscovered.named")
        .addValue("waypoint", displayName)
        .exception();
  }

  static CommandSyntaxException alreadyDiscovered(Audience viewer, User user, Waypoint waypoint) {
    return Messages.render("waypoints.errors.alreadyDiscovered")
        .addValue("waypoint", waypoint.nonNullDisplayName())
        .addValue("player", user)
        .exception(viewer);
  }

  static CommandSyntaxException notDiscovered(Audience viewer, User user) {
    return Messages.render("waypoints.errors.notDiscovered.other")
        .addValue("player", user)
        .exception(viewer);
  }
}