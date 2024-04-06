package net.arcadiusmc.serverlist;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.utils.MonthDayPeriod;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;
import org.slf4j.Logger;

/**
 * Dynamically/randomly changes the server icon
 */
@Getter
public class ServerListDisplay {
  private static final Logger LOGGER = Loggers.getLogger();

  public static final Comparator<Holder<DisplayEntry>> ENTRY_COMPARATOR = Holder.comparingByValue();

  /** Key of the default display entry */
  public static final String DEFAULT = "default";

  /** Registry of all display entries */
  private final Registry<DisplayEntry> registry = Registries.newRegistry();

  /** Cached list of entries are applicable to the current date */
  private final List<Holder<DisplayEntry>> dateCache = new ArrayList<>();

  private final Path iconDirectory;
  private final Path loaderFile;

  private final Random random;

  ServerListDisplay() {
    this.iconDirectory = PathUtil.pluginPath("icons");
    this.loaderFile = PathUtil.pluginPath("serverlist.toml");

    this.random = new Random();

    saveDefaults();
  }

  void saveDefaults() {
    PluginJar.saveResources("serverlist.toml", loaderFile);
  }

  /** Caches all registered entries that are applicable to the current date */
  void cacheDateEntries() {
    dateCache.clear();

    LocalDate today = LocalDate.now();
    registry.entries()
        .stream()
        .filter(holder -> {
          if (holder.getKey().equals(DEFAULT)) {
            return false;
          }

          var val = holder.getValue();

          if (val.getIcons().isEmpty() && val.getMotdPart() == null) {
            return false;
          }

          var period = val.getPeriod();
          return period == null || period.contains(today);
        })
        .forEach(dateCache::add);

    dateCache.sort(ENTRY_COMPARATOR);
  }

  ListDisplayData getCurrent() {
    if (registry.isEmpty()) {
      return new ListDisplayData();
    }

    var date = LocalDate.now();
    ListDisplayData displayData = new ListDisplayData();

    for (Holder<DisplayEntry> i : dateCache) {
      var val = i.getValue();

      if (!val.shouldUse(date, random)) {
        continue;
      }

      displayData.fillValues(val, random);
    }

    // Either MOTD, icon or both are missing, get the default entry
    // and fill any missing data with it
    Optional<Holder<DisplayEntry>> def = registry.getHolder(DEFAULT);

    if (def.isPresent()) {
      var defaultValue = def.get().getValue();
      displayData.fillValues(defaultValue, random);
    }

    return displayData;
  }

  public void load() {
    registry.clear();

    SerializationHelper.readAsJson(loaderFile, wrapper -> {
      for (var e : wrapper.entrySet()) {
        if (!Registries.isValidKey(e.getKey())) {
          LOGGER.warn("Invalid icon key found! '{}'", e.getKey());
          continue;
        }

        DisplayEntry icon;
        JsonElement element = e.getValue();

        if (element.isJsonPrimitive() || element.isJsonArray()) {
          icon = new DisplayEntry(null, readIconList(element), null, null, 0, -1,null);
        } else {
          JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());

          List<CachedServerIcon> icons = readIconList(json.get("icons"));
          MonthDayPeriod period = MonthDayPeriod.ALL;
          Script condition = null;
          Component motdPart = null;
          int prio = json.getInt("priority", 0);
          int protocol = json.getInt("protocol_override", -1);
          String versionText = json.getString("version_text", null);

          if (json.has("condition")) {
            condition = Scripts.loadScript(json.get("condition"), true);
            condition.compile();
          }

          if (json.has("period")) {
            period = MonthDayPeriod.load(json.get("period"));
          }

          if (json.has("motd")) {
            motdPart = json.getComponent("motd");
          }

          if (icons.isEmpty() && motdPart == null) {
            LOGGER.warn(
                "Found display entry with no MOTD section and no icons: {}",
                e.getKey()
            );
            continue;
          }

          icon = new DisplayEntry(period, icons, condition, motdPart, prio, protocol, versionText);
        }

        registry.register(e.getKey(), icon);
        LOGGER.debug("Loaded serverlist display entry {}", e.getKey());
      }
    });

    cacheDateEntries();
  }

  private List<CachedServerIcon> readIconList(JsonElement element) {
    if (element == null) {
      return List.of();
    }

    if (element.isJsonPrimitive()) {
      var stringPath = element.getAsString();
      Path path = iconDirectory.resolve(stringPath);
      var icon = loadImage(path);

      if (icon == null) {
        return List.of();
      }

      return List.of(icon);
    }

    var arr = element.getAsJsonArray();
    List<CachedServerIcon> icons = new ObjectArrayList<>(arr.size());

    for (int i = 0; i < arr.size(); i++) {
      var stringPath = arr.get(i).getAsString();
      Path path = iconDirectory.resolve(stringPath);
      var icon = loadImage(path);

      if (icon == null) {
        continue;
      }

      icons.add(icon);
    }

    return icons;
  }

  private CachedServerIcon loadImage(Path path) {
    if (Files.notExists(path)) {
      LOGGER.warn("Icon '{}' doesn't exist!", path);
      return null;
    }

    try {
      return Bukkit.loadServerIcon(path.toFile());
    } catch (Exception exc) {
      LOGGER.error("Couldn't load server icon '{}'", path, exc);
      return null;
    }
  }

}