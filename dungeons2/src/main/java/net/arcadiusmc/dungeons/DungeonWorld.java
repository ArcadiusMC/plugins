package net.arcadiusmc.dungeons;

import java.nio.file.Files;
import java.nio.file.Path;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.math.Vectors;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.spongepowered.math.vector.Vector2i;

public final class DungeonWorld {
  private DungeonWorld() {}

  public static final String WORLD_NAME = "world_dungeons";

  public static final int SECTION_CELL_BITSHIFT = 11;
  public static final int SECTION_CELL_SIZE = 1 << SECTION_CELL_BITSHIFT;
  public static final int SECTION_CELL_AREA = SECTION_CELL_SIZE * SECTION_CELL_SIZE;

  public static World get() {
    return Bukkit.getWorld(WORLD_NAME);
  }

  public static World reset() {
    World world = get();

    if (world != null) {
      removeWorld(world);
    } else {
      Path worldFile = Bukkit.getWorldContainer().toPath().resolve(WORLD_NAME);

      if (Files.exists(worldFile)) {
        PathUtil.safeDelete(worldFile);
      }
    }

    return new WorldCreator(WORLD_NAME)
        .generator("VoidGen")
        .type(WorldType.FLAT)
        .keepSpawnLoaded(TriState.FALSE)
        .environment(World.Environment.NORMAL)
        .createWorld();
  }

  public static Vector2i toCell(int worldX, int worldZ) {
    return Vector2i.from(worldX >> SECTION_CELL_BITSHIFT, worldZ >> SECTION_CELL_BITSHIFT);
  }

  public static Vector2i toWorld(int cellX, int cellZ) {
    return Vector2i.from(cellX << SECTION_CELL_BITSHIFT, cellZ << SECTION_CELL_BITSHIFT);
  }

  public static long toCellId(int worldX, int worldZ) {
    int cx = worldX << SECTION_CELL_BITSHIFT;
    int cz = worldZ << SECTION_CELL_BITSHIFT;
    return Vectors.toChunkLong(cx, cz);
  }

  public static Vector2i fromCellId(long cellId) {
    Vector2i v = Vectors.fromChunkLong(cellId);
    return toWorld(v.x(), v.y());
  }

  static void removeWorld(World world) {
    Worlds.desroyWorld(world);
  }
}