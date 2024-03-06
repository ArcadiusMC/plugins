package net.arcadiusmc.waypoints.type;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.Waypoints;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class WildernessType extends WaypointType {

  static final Material[] COLUMN = {
      Material.STONE_BRICKS,
      Material.STONE_BRICKS,
      Material.POLISHED_ANDESITE
  };

  public WildernessType() {
    super("Wilderness", COLUMN);
  }

  @Override
  public boolean isDestroyed(Waypoint waypoint) {
    return WaypointTypes.isDestroyed(getColumn(), waypoint.getPosition(), waypoint.getWorld());
  }

  @Override
  protected boolean internalIsBuildable() {
    return false;
  }

  @Override
  public @NotNull Bounds3i createBounds() {
    return WaypointTypes.PLAYER.createBounds();
  }

  @Override
  public Optional<CommandSyntaxException> isValid(Waypoint waypoint) {
    return Waypoints.isValidWaypointArea(
        waypoint.getPosition(),
        waypoint.getType(),
        waypoint.getWorld(),
        false
    );
  }
}
