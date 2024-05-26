package net.arcadiusmc.dungeons;

import com.mojang.serialization.DataResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.function.Function;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.gate.GateType;
import net.arcadiusmc.dungeons.room.RoomType;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructureFillConfig;
import net.arcadiusmc.utils.Time;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.slf4j.Logger;

@Getter
public class LevelDataStorage {

  private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR)
      .appendLiteral('_')
      .appendValue(ChronoField.MONTH_OF_YEAR, 2)
      .appendLiteral('_')
      .appendValue(ChronoField.DAY_OF_MONTH, 2)
      .appendLiteral('_')
      .appendValue(ChronoField.HOUR_OF_DAY)
      .appendLiteral('_')
      .appendValue(ChronoField.MINUTE_OF_HOUR)
      .toFormatter();

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path directory;
  private final Path archiveDirectory;

  private final Path activeLevel;

  private final Path roomJson;
  private final Path gateJson;

  LevelDataStorage(Path directory) {
    this.directory = directory;
    this.archiveDirectory = directory.resolve("archives");
    this.activeLevel = directory.resolve("level.dat");

    this.roomJson = directory.resolve("rooms.json");
    this.gateJson = directory.resolve("gates.json");

    saveDefaults();
  }

  void saveDefaults() {
    PluginJar.saveResources("dungeons", directory);
  }

  public Path getPath(long creationTime, int i) {
    LocalDateTime localTime = Time.localTime(creationTime);
    String strPath = FORMATTER.format(localTime);

    if (i > 0) {
      strPath += " (" + i + ")";
    }

    strPath += ".dat";
    return archiveDirectory.resolve(strPath);
  }

  public void archiveLevelStructure(DungeonLevel level, long creationTime) {
    Path path = null;
    int i = 0;

    while (path == null || Files.exists(path)) {
      path = getPath(creationTime, i);
    }

    BlockStructure structure = new BlockStructure();
    StructureFillConfig config = StructureFillConfig.builder()
        .blockPredicate(block -> !block.getType().isAir())
        .area(level.getChunkMap().getTotalArea().toWorldBounds(DungeonWorld.get()))
        .build();

    structure.fill(config);

    var header = structure.getHeader();
    header.putString("createdDate", JsonUtils.DATE_FORMAT.format(new Date(creationTime)));

    CompoundTag levelData = BinaryTags.compoundTag();
    level.save(levelData);
    header.put("level_data", levelData);

    SerializationHelper.writeTagFile(path, structure::save);
  }

  public void loadRooms(Registry<RoomType> roomTypeRegistry) {
    loadPieceTypes(getRoomJson(), roomTypeRegistry, RoomType::loadType);
  }

  public void loadGates(Registry<GateType> gateTypeRegistry) {
    loadPieceTypes(getGateJson(), gateTypeRegistry, GateType::loadType);
  }

  public <T extends PieceType<?>> void loadPieceTypes(
      Path path,
      Registry<T> registry,
      Function<JsonWrapper, DataResult<T>> function
  ) {
    SerializationHelper.readJsonFile(path, wrapper -> {
      for (var e: wrapper.entrySet()) {
        var key = e.getKey();

        if (!Registries.isValidKey(key)) {
          LOGGER.error("Cannot read piece type! Invalid key '{}'", key);
          continue;
        }

        if (!e.getValue().isJsonObject()) {
          LOGGER.error("Cannot read type '{}', not a JSON object", key);
          continue;
        }

        var obj = e.getValue().getAsJsonObject();

        function.apply(JsonWrapper.wrap(obj))
            .mapError(s -> "Cannot read type '" + key + "', " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(t -> {
              registry.register(key, t);
            });
      }
    });
  }
}