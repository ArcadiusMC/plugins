package net.arcadiusmc.dungeons;

import java.nio.file.Files;
import java.nio.file.Path;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.utils.io.PathUtil;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

public final class DungeonWorld {
  private DungeonWorld() {}

  public static final String WORLD_NAME = "world_dungeons";

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

    world = new WorldCreator(WORLD_NAME)
        .generator("VoidGen")
        .type(WorldType.FLAT)
        .keepSpawnLoaded(TriState.FALSE)
        .environment(World.Environment.NORMAL)
        .createWorld();

    world.setDifficulty(Difficulty.HARD);
    world.setTime(6000L);

    world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    world.setGameRule(GameRule.DO_FIRE_TICK, false);
    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    world.setGameRule(GameRule.MOB_GRIEFING, false);
    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

    return world;
  }

  static void removeWorld(World world) {
    Worlds.desroyWorld(world);
  }
}