package net.arcadiusmc.punish;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class PunishManager {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Map<UUID, PunishEntry> entries = new HashMap<>();
  private boolean serverStarted = false;

  private final Path file;

  public PunishManager(Path file) {
    this.file = file;
  }

  public Optional<PunishEntry> getOptionalEntry(UUID playerId) {
    return Optional.ofNullable(entries.get(playerId));
  }

  public @NotNull PunishEntry getEntry(@NotNull UUID playerId) {
    UserService service = Users.getService();

    if (service.getLookup().getEntry(playerId) == null) {
      throw new IllegalArgumentException("Non player ID given: " + playerId);
    }

    PunishEntry entry = entries.get(playerId);
    if (entry != null) {
      return entry;
    }

    entry = new PunishEntry(playerId);
    addEntry(entry);

    return entry;
  }

  private void addEntry(PunishEntry entry) {
    if (entries.containsKey(entry.getPlayerId())) {
      throw new IllegalArgumentException("Dual entry addition, id=" + entry.getPlayerId());
    }

    if (serverStarted) {
      entry.validate();
    }

    entries.put(entry.getPlayerId(), entry);
  }

  public void clear() {
    for (PunishEntry value : entries.values()) {
      value.invalidate();
    }

    entries.clear();
  }

  public void onServerStarted() {
    serverStarted = true;
    entries.forEach((uuid, entry) -> entry.validate());
  }

  public void save() {
    Optional<CompoundTag> opt = PunishEntry.ENTRIES.encodeStart(TagOps.OPS, entries)
        .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
        .mapError(s -> "Failed to save userdata to " + file + ": " + s)
        .resultOrPartial(LOGGER::error);

    if (opt.isEmpty()) {
      return;
    }

    CompoundTag tag = opt.get();
    SerializationHelper.writeTag(file, tag);
  }

  public void load() {
    clear();
    SerializationHelper.readTagFile(file, this::loadFrom);
  }

  private void loadFrom(CompoundTag tag) {
    Optional<Map<UUID, PunishEntry>> opt = PunishEntry.ENTRIES.parse(TagOps.OPS, tag)
        .mapError(s -> "Failed to load userdata from " + file + ": " + s)
        .resultOrPartial(LOGGER::error);

    if (opt.isEmpty()) {
      return;
    }

    Map<UUID, PunishEntry> map = opt.get();
    entries.putAll(map);

    for (PunishEntry value : entries.values()) {
      value.validate();
    }
  }
}
