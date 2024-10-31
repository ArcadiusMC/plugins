package net.arcadiusmc.waypoints;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.waypoints.type.WaypointTypes;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector3i;

public class AutoGen {

  static final int MIN_GRID_SIZE = 10;
  static final int MIN_EMPTY_RADIUS = 200;

  private static final Logger LOGGER = Loggers.getLogger();

  public static final NamespacedKey CHUNK_MARKER
      = new NamespacedKey("arcadiusmc", "has_generated_waypoint");

  public static boolean serverInitialized = false;

  public static void maybePlace(Chunk chunk, WaypointManager manager) {
    if (!serverInitialized) {
      return;
    }

    WaypointConfig config = manager.config();

    if (isDisabled(config, chunk.getWorld())) {
      return;
    }

    if (chunk.getPersistentDataContainer().has(CHUNK_MARKER)) {
      return;
    }

    Vector2i min = Vector2i.from(Vectors.toBlock(chunk.getX()), Vectors.toBlock(chunk.getZ()));
    Vector2i max = min.add(Vectors.CHUNK_SIZE, Vectors.CHUNK_SIZE);

    World world = chunk.getWorld();

    Vector2i waypointPos = nearestGridCenter(min, config.wildernessWaypointGrid);

    if ((waypointPos.x() < min.x() || waypointPos.y() < min.y())
        || (waypointPos.x() > max.x() || waypointPos.y() > max.y())
    ) {
      return;
    }

    Tasks.runLater(() -> AutoGen.placeAutoWaypoint(world, manager, waypointPos), 20);
  }

  public static Vector2i nearestGridCenter(Vector2i wpos, Vector2i gridSize) {
    int x = wpos.x();
    int z = wpos.y();

    int gx = gridSize.x();
    int gz = gridSize.y();

    int cx = f(x, gx);
    int cz = f(z, gz);

    return new Vector2i(cx, cz);
  }

  public static int f(double x, double g) {
    //
    // I WILL FUCKING KILL SOMEONE, WHY DOES THIS FUCNTION **NEED** DOUBLES
    // IF GIVEN ANY OTHER DATA TYPE IT RETURNS THE WRONG RESULT, WHY ARE
    // 64BIT FLOATING POINT NUMBERS REQUIRED FOR CORRECTNESS WHAT THE FUCK
    //
    // integers? Nope, wrong
    // longs? ALSO WRONG
    //
    // BUT DOUBLES???? CORRECT DING DING DING DING DING
    //
    double off = x + Math.floor(g / 2);
    double girdPos = Math.floor(off / g);
    return (int) Math.floor(girdPos * g);
  }

  public static void placeAutoWaypoint(World world, WaypointManager manager, Vector2i waypointPos) {
    Vector3i position = toVec3i(waypointPos, world);

    ObjectDoublePair<Waypoint> nearest = manager.getChunkMap()
        .findNearest(position.toDouble(), world);

    // Sometimes, it might generate double waypoints,
    // this prevents that don't ask me why that happens,
    // I have no idea why
    if (nearest.left() != null && nearest.rightDouble() < MIN_EMPTY_RADIUS) {
      return;
    }

    LOGGER.info("Generating waypoint at {}", waypointPos);

    Chunk chunk = world.getChunkAt(
        Vectors.toChunk(waypointPos.x()),
        Vectors.toChunk(waypointPos.y())
    );

    // Set chunk marker to prevent re-generation
    chunk.getPersistentDataContainer().set(CHUNK_MARKER, PersistentDataType.BYTE, (byte) 1);

    Waypoint waypoint = new Waypoint();
    waypoint.setType(WaypointTypes.WILDERNESS);
    waypoint.setPosition(position, world);

    manager.addWaypoint(waypoint);

    clearWaypointArea(waypoint.getBounds(), world);

    Vector3i platform = waypoint.getPlatform();
    if (platform != null) {
      Waypoints.placePlatform(world, platform);
    }

    waypoint.placeColumn();
    waypoint.update(true);
  }

  private static void clearWaypointArea(Bounds3i bounds3i, World world) {
    for (Block block : bounds3i.toWorldBounds(world)) {
      block.setType(Material.AIR, false);
    }
  }

  public static Vector3i toVec3i(Vector2i pos, World world) {
    int highestY = world.getHighestBlockYAt(pos.x(), pos.y(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
    return Vector3i.from(pos.x(), highestY + 1, pos.y());
  }

  public static Vector2i nearestGridWaypoint(Vector2i pos, World world) {
    WaypointConfig config = WaypointManager.getInstance().config();

    if (isDisabled(config, world)) {
      return null;
    }

    Vector2i gridSize = config.wildernessWaypointGrid;
    return nearestGridCenter(pos, gridSize);
  }

  public static boolean isDisabled(WaypointConfig config, World world) {
    if (config.autoGenWorlds.length < 1) {
      return true;
    }

    Vector2i gridSize = config.wildernessWaypointGrid;
    if (gridSize.x() < MIN_GRID_SIZE || gridSize.y() < MIN_GRID_SIZE) {
      return true;
    }

    return !ArrayUtils.contains(config.autoGenWorlds, world.getName());
  }
}
