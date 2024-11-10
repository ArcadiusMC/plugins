package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.arcadiusmc.dungeons.SpawnerCodecs;
import net.arcadiusmc.utils.WeightedList;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import org.bukkit.Material;
import org.bukkit.block.spawner.SpawnerEntry;

@Getter @Setter @ToString
public class SpawnerConfig {

  static final List<BlockFilterArgument.Result> DEFAULT_CAN_REPLACE = List.of(
      BlockFilters.create(Material.SHORT_GRASS),
      BlockFilters.create(Material.TALL_GRASS),
      BlockFilters.parse("#air")
  );

  static final Codec<SpawnerConfig> CODEC = ExistingObjectCodec.createCodec(
      SpawnerConfig::new,
      builder -> {
        builder.optional("entries", WeightedList.codec(SpawnerSettings.CODEC))
            .setter(SpawnerConfig::setEntries)
            .getter(SpawnerConfig::getEntries);

        builder.optional("can-replace", BlockFilters.CODEC.listOf())
            .setter(SpawnerConfig::setCanReplace)
            .getter(SpawnerConfig::getCanReplace);

        builder.optional("override-all", Codec.BOOL)
            .setter(SpawnerConfig::setOverrideAll)
            .getter(SpawnerConfig::isOverrideAll);
      }
  );

  private WeightedList<SpawnerSettings> entries = new WeightedList<>();
  private List<BlockFilterArgument.Result> canReplace = DEFAULT_CAN_REPLACE;
  private boolean overrideAll = false;

  @Getter @Setter @ToString
  public static class SpawnerSettings {

    static final Codec<SpawnerSettings> CODEC = ExistingObjectCodec.createCodec(
        SpawnerSettings::new,
        builder -> {
          builder.optional("min-spawn-delay", Codec.INT)
              .setter(SpawnerSettings::setMinSpawnDelay)
              .getter(SpawnerSettings::getMinSpawnDelay);

          builder.optional("max-spawn-delay", Codec.INT)
              .setter(SpawnerSettings::setMaxSpawnDelay)
              .getter(SpawnerSettings::getMaxSpawnDelay);

          builder.optional("spawn-count", Codec.INT)
              .setter(SpawnerSettings::setSpawnCount)
              .getter(SpawnerSettings::getSpawnCount);

          builder.optional("spawn-range", Codec.INT)
              .setter(SpawnerSettings::setSpawnRange)
              .getter(SpawnerSettings::getSpawnRange);

          builder.optional("max-nearby-entities", Codec.INT)
              .setter(SpawnerSettings::setMaxNearby)
              .getter(SpawnerSettings::getMaxNearby);

          builder.optional("required-player-range", Codec.INT)
              .setter(SpawnerSettings::setRequiredPlayerRange)
              .getter(SpawnerSettings::getRequiredPlayerRange);

          builder.optional("entities", SpawnerCodecs.SPAWNER_ENTRY.listOf())
              .setter(SpawnerSettings::setEntries)
              .getter(SpawnerSettings::getEntries);
        }
    );

    private int minSpawnDelay = 20;
    private int maxSpawnDelay = 40;

    private int spawnCount = 1;
    private int maxNearby = 5;
    private int requiredPlayerRange = 10;
    private int spawnRange = 3;

    private List<SpawnerEntry> entries = List.of();
  }
}
