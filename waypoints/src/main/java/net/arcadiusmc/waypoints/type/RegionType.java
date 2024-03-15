package net.arcadiusmc.waypoints.type;

import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.waypoints.Waypoint;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3i;

public class RegionType extends WaypointType {

  static final String TAG = "waypoint_column_entity";

  static final Material[] COLUMN = {
      Material.BARRIER,
      Material.BARRIER,
      Material.LODESTONE
  };

  public RegionType() {
    super("Region", COLUMN);
  }

  @Override
  protected boolean internalIsBuildable() {
    return false;
  }

  @Override
  public @NotNull Bounds3i createBounds() {
    return WaypointTypes.ADMIN.createBounds();
  }

  @Override
  public void onWaypointAdded(Waypoint waypoint) {
    spawnEntity(waypoint.getWorld(), waypoint.getPosition());
  }

  @Override
  public void onPreMove(Waypoint waypoint, Vector3i newPosition, World newWorld) {
    killEntity(waypoint.getWorld(), waypoint.getPosition());
  }

  @Override
  public void onPostMove(Waypoint waypoint) {
    spawnEntity(waypoint.getWorld(), waypoint.getPosition());
  }

  @Override
  public void onPostCreate(Waypoint waypoint, User creator) {
    spawnEntity(waypoint.getWorld(), waypoint.getPosition());
  }

  @Override
  public void onDelete(Waypoint waypoint) {
    killEntity(waypoint.getWorld(), waypoint.getPosition());
  }

  private void spawnEntity(World world, Vector3i position) {
    if (world == null) {
      return;
    }

    killEntity(world, position);

    Location location = toLocation(world, position);

    world.spawn(location, ItemDisplay.class, display -> {
      ItemStack item = new ItemStack(Material.BAMBOO_BUTTON);
      item.editMeta(meta -> {
        meta.setCustomModelData(10090001);
      });

      display.setItemStack(item);
      display.addScoreboardTag(TAG);
    });
  }

  private void killEntity(World world, Vector3i position) {
    if (world == null) {
      return;
    }

    Location location = toLocation(world, position);
    Chunk chunk = location.getChunk();
    Entity[] entities = chunk.getEntities();

    for (Entity entity : entities) {
      if (!entity.getScoreboardTags().contains(TAG)) {
        continue;
      }

      entity.remove();
    }
  }

  private Location toLocation(World world, Vector3i position) {
    return new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
  }
}
