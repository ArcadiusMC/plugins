package net.arcadiusmc.markets;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.ScoreIntMap;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import org.slf4j.Logger;

public class Debts {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<Object2IntMap<UUID>> CODEC
      = Codec.unboundedMap(ExtraCodecs.STRING_UUID, Codec.INT)
      .xmap(Object2IntOpenHashMap::new, Function.identity());

  @Getter
  private final ScoreIntMap<UUID> debts = new ScoreIntMap<>();

  private final Path file;

  public Debts(MarketsPlugin plugin) {
    this.file = plugin.getDataFolder().toPath().resolve("debts.dat");
  }

  public void load() {
    debts.clear();

    SerializationHelper.readTagFile(file, tag -> {
      CODEC.parse(TagOps.OPS, tag)
          .mapError(string -> "Failed to load debts from " + file + ": " + string)
          .resultOrPartial(LOGGER::error)
          .ifPresent(map -> {
            map.forEach(debts::set);
            debts.setDirty(false);
          });
    });
  }

  public void save() {
    if (!debts.isDirty()) {
      return;
    }

    CODEC.encodeStart(TagOps.OPS, debts.toMap())
        .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
        .mapError(string -> "Failed to save debts to " + file + ": " + string)
        .resultOrPartial(LOGGER::error)
        .ifPresent(tag -> {
          SerializationHelper.writeTag(file, tag);
          debts.setDirty(false);
        });
  }
}
