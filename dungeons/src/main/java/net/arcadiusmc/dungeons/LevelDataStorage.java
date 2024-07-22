package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Map;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.dungeons.gate.GateType;
import net.arcadiusmc.dungeons.room.RoomType;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructureFillConfig;
import net.arcadiusmc.structure.buffer.BlockBuffer;
import net.arcadiusmc.structure.buffer.BlockBuffers;
import net.arcadiusmc.utils.Time;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.math.Bounds3i;
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

  private final Path roomsFile;
  private final Path gatesFile;

  LevelDataStorage(Path directory) {
    this.directory        = directory;
    this.archiveDirectory = directory.resolve("archives");
    this.activeLevel      = directory.resolve("level.dat");

    this.roomsFile        = directory.resolve("rooms.yml");
    this.gatesFile        = directory.resolve("gates.yml");
  }

  void saveDefaults() {
    PluginJar.saveResources("gates.yml", gatesFile);
    PluginJar.saveResources("rooms.yml", roomsFile);
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

  public void archiveLevelStructure(DungeonStructure level, long creationTime) {
    Path path = null;
    int i = 0;

    while (path == null || Files.exists(path)) {
      path = getPath(creationTime, i++);
    }

    Bounds3i totalArea = level.getChunkMap().getTotalArea();
    BlockBuffer buf = BlockBuffers.allocate(totalArea);

    BlockStructure structure = new BlockStructure();
    StructureFillConfig config = StructureFillConfig.builder()
        .blockPredicate(block -> !block.getType().isAir())
        .buffer(buf)
        .area(totalArea.toWorldBounds(Worlds.overworld()))
        .build();

    structure.fill(config);

    CompoundTag header = structure.getHeader();
    header.putString("created_date", JsonUtils.DATE_FORMAT.format(new Date(creationTime)));
    header.putLong("created_timestamp", creationTime);

    CompoundTag levelData = BinaryTags.compoundTag();
    level.save(levelData);
    header.put("level_data", levelData);

    SerializationHelper.writeTagFile(path, structure::save);
  }

  public void loadRooms(Registry<RoomType> roomTypeRegistry) {
    loadPieceTypes(getRoomsFile(), roomTypeRegistry, RoomType.CODEC);
  }

  public void loadGates(Registry<GateType> gateTypeRegistry) {
    loadPieceTypes(getGatesFile(), gateTypeRegistry, GateType.CODEC);
  }

  public <T extends PieceType<?>> void loadPieceTypes(
      Path path,
      Registry<T> registry,
      Codec<T> codec
  ) {
    Codec<Map<String, T>> mapCodec = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, codec);

    SerializationHelper.readAsJson(path, json -> {
      mapCodec.parse(JsonOps.INSTANCE, json)
          .mapError(s -> "Failed to read piece types: " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(map -> map.forEach((key, type) -> {
            if (key.equalsIgnoreCase("example")) {
              return;
            }

            registry.register(key, type);
          }));
    });
  }
}