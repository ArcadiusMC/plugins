package net.arcadiusmc.punish;

import com.mojang.serialization.Codec;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.CompoundTag;
import org.slf4j.Logger;

public class JailManager {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final Codec<Map<String, JailCell>> CELL_MAP_CODEC
      = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, JailCell.CODEC);

  @Getter
  private final Registry<JailCell> cells = Registries.newRegistry();

  private final Path file;

  public JailManager(Path file) {
    this.file = file;
  }

  public void clear() {
    cells.clear();
  }

  public void save() {
    Optional<CompoundTag> opt = CELL_MAP_CODEC.encodeStart(TagOps.OPS, cells.toMap())
        .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
        .mapError(s -> "Failed to save jail cells to " + file + ": " + s)
        .resultOrPartial(LOGGER::error);

    if (opt.isEmpty()) {
      return;
    }

    CompoundTag tag = opt.get();
    SerializationHelper.writeTag(file, tag);
  }

  public void load() {
    clear();

    SerializationHelper.readTagFile(file, tag -> {
      Optional<Map<String, JailCell>> opt = CELL_MAP_CODEC.parse(TagOps.OPS, tag)
          .mapError(s -> "Failed to load jail cells from " + file + ": " + s)
          .resultOrPartial(LOGGER::error);

      if (opt.isEmpty()) {
        return;
      }

      Map<String, JailCell> cellMap = opt.get();
      cellMap.forEach(cells::register);
    });
  }
}
