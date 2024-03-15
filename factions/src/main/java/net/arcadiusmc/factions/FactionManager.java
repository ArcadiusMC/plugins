package net.arcadiusmc.factions;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.jetbrains.annotations.NotNull;

public class FactionManager {

  private final Path directory;
  private final FactionsPlugin plugin;

  private final Map<String, Faction> factionMap = new HashMap<>();

  public FactionManager(FactionsPlugin plugin) {
    this.plugin = plugin;
    this.directory = plugin.getDataFolder().toPath().resolve("faction-data");
  }

  public void addFaction(Faction faction) {
    Objects.requireNonNull(faction, "Null faction");

    String key = faction.getKey();
    Faction existing = factionMap.get(key);

    if (existing != null) {
      throw new IllegalStateException("Faction with key '" + key + "' already exists");
    }

    factionMap.put(key, faction);
  }

  public void removeFaction(Faction faction) {
    factionMap.remove(faction.getKey());
  }

  public void deleteFaction(Faction faction) {
    removeFaction(faction);

    Path file = getFactionFile(faction);
    PathUtil.safeDelete(file);
  }

  public Faction getFaction(String key) {
    return factionMap.get(key);
  }

  public Set<String> getFactionNames() {
    return Collections.unmodifiableSet(factionMap.keySet());
  }

  public Collection<Faction> getFactions() {
    return Collections.unmodifiableCollection(factionMap.values());
  }

  public Faction getCurrentFaction(@NotNull UUID playerId) {
    Objects.requireNonNull(playerId, "Null playerId");

    for (Faction value : factionMap.values()) {
      FactionMember member = value.getActiveMember(playerId);

      if (member != null) {
        return value;
      }
    }

    return null;
  }

  private Path getFactionFile(Faction faction) {
    String fileName = faction.getKey() + ".dat";
    return directory.resolve(fileName);
  }

  public void save() {
    factionMap.values().forEach(this::save);
  }

  public void load() {
    factionMap.clear();

    List<String> files = PathUtil.findAllFiles(
        directory,
        false,
        path -> path.toString().endsWith(".dat")
    );

    if (files.isEmpty()) {
      return;
    }

    for (String file : files) {
      Path factionFile = directory.resolve(file + ".dat");

      SerializationHelper.readTagFile(factionFile, tag -> {
        Faction faction = new Faction(file);
        faction.load(tag);
        addFaction(faction);
      });
    }
  }

  public void save(Faction faction) {
    Path path = getFactionFile(faction);
    SerializationHelper.writeTagFile(path, faction::save);
  }
}
