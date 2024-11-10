package net.arcadiusmc.dungeons.gen;

import java.util.List;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.gen.SpawnerConfig.SpawnerSettings;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

public class SpawnerDecorator extends Decorator<SpawnerConfig> {

  private static final Logger LOGGER = Loggers.getLogger();

  public static final DecoratorType<SpawnerDecorator, SpawnerConfig> TYPE
      = DecoratorType.create(SpawnerConfig.CODEC, SpawnerDecorator::new);

  public SpawnerDecorator(SpawnerConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.getEntries().isEmpty()) {
      return;
    }

    List<GeneratorFunction> spawners = getFunctions(LevelFunctions.SPAWNER);

    for (GeneratorFunction spawner : spawners) {
      Vector3i pos = spawner.getPosition();
      placeSpawner(pos.x(), pos.y(), pos.z());
    }
  }

  private SpawnerSettings pickRandomEntry() {
    if (config.getEntries().isEmpty()) {
      return null;
    }
    return config.getEntries().get(random);
  }

  private void placeSpawner(int x, int y, int z) {
    if (!config.isOverrideAll() && !matchesAny(x, y, z, config.getCanReplace())) {
      return;
    }

    SpawnerSettings settings = pickRandomEntry();
    assert settings != null;

    BlockData data = Material.SPAWNER.createBlockData();
    CreatureSpawner spawner = (CreatureSpawner) data.createBlockState();

    spawner.setMinSpawnDelay(settings.getMinSpawnDelay());
    spawner.setMaxSpawnDelay(settings.getMaxSpawnDelay());

    spawner.setSpawnCount(settings.getSpawnCount());
    spawner.setMaxNearbyEntities(settings.getMaxNearby());
    spawner.setRequiredPlayerRange(settings.getRequiredPlayerRange());
    spawner.setSpawnRange(settings.getSpawnRange());

    spawner.setPotentialSpawns(settings.getEntries());

    setBlock(x, y, z, spawner);
  }
}
