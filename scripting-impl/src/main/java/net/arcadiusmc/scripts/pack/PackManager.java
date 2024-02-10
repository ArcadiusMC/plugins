package net.arcadiusmc.scripts.pack;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Unit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.scripts.ScriptManager;
import net.arcadiusmc.scripts.ScriptService;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

@Getter
public class PackManager {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Path directory;
  private final ScriptService service;

  private final Registry<ScriptPack> packs = Registries.newRegistry();
  private final List<Holder<ScriptPack>> loadOrder = new ArrayList<>();

  public boolean started = false;

  public PackManager(ScriptManager service, Path directory) {
    this.service = service;
    this.directory = directory;

    packs.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<ScriptPack> value) {
        sortLoadOrder();
      }

      @Override
      public void onUnregister(Holder<ScriptPack> value) {
        sortLoadOrder();
      }
    });
  }

  private void sortLoadOrder() {
    List<Holder<ScriptPack>> unsorted = new ArrayList<>();

    outer: for (Holder<ScriptPack> entry : packs.entries()) {
      List<String> dependsOn = entry.getValue().getMeta().getRequiredScripts();

      for (String s : dependsOn) {
        if (packs.contains(s)) {
          continue;
        }

        LOGGER.error("Cannot place script pack '{}' into load order! Missing dependency '{}'",
            entry.getKey(), s
        );

        continue outer;
      }

      unsorted.add(entry);
    }

    List<Holder<ScriptPack>> sorted = TopologicalSort.sort(unsorted);

    loadOrder.clear();
    loadOrder.addAll(sorted);
  }

  public void activate() {
    LOGGER.info("Activating script packs");
    started = true;

    for (Holder<ScriptPack> entry : loadOrder) {
      activate(entry);
    }
  }

  private boolean activate(Holder<ScriptPack> entry) {
    if (!testDependents(entry)) {
      return false;
    }

    Result<Unit> result = entry.getValue().activate();

    result.apply(string -> {
      LOGGER.error("Failed to activate script pack '{}': {}", entry.getKey(), string);
    }, unit -> {
      LOGGER.debug("Activated pack '{}', creating exports...", entry.getKey());
      entry.getValue().createExports();
    });

    return !result.isError();
  }

  private boolean testDependents(Holder<ScriptPack> holder) {
    List<String> dependsOn = holder.getValue().getMeta().getRequiredScripts();

    if (dependsOn.isEmpty()) {
      return true;
    }

    List<String> missing = new ArrayList<>(dependsOn.size());

    for (String s : dependsOn) {
      Optional<ScriptPack> opt = packs.get(s);

      if (opt.isEmpty()) {
        missing.add(s);
        continue;
      }

      var pack = opt.get();

      if (pack.isActivated()) {
        continue;
      }

      missing.add(s);
    }

    if (missing.isEmpty()) {
      return true;
    }

    LOGGER.error("Cannot activate pack '{}': Missing script packs: {}",
        holder.getKey(), Joiner.on(", ").join(missing)
    );

    return false;
  }

  public void reload() {
    close();

    PluginJar.saveResources("pack-example", directory.resolve("pack-example"));

    PathUtil.iterateDirectory(directory, false, true, this::loadPack)
        .mapError(string -> "Error iterating over script packs directory: " + string)
        .resultOrPartial(LOGGER::error);
  }

  public void loadPack(Path file) {
    String fName = file.getFileName().toString();

    // Don't load the example pack lol
    if (fName.equals("pack-example")) {
      return;
    }

    if (!Files.isDirectory(file)) {
      LOGGER.debug("File in packs folder, '{}', was not a directory", file);
      return;
    }

    Path metaFile = file.resolve("script-pack.toml");

    if (!Files.exists(metaFile)) {
      LOGGER.warn("Cannot load script pack '{}', no 'script-pack.toml' file exists", fName);
      return;
    }

    SerializationHelper.readAsJson(metaFile, json -> {
      loadPackFromMeta(json, file, fName);
    });
  }

  private void loadPackFromMeta(JsonElement element, Path directory, String key) {
    if (packs.contains(key)) {
      LOGGER.error("Duplicate definition of script pack '{}'", key);
      return;
    }

    Optional<PackMeta> result = PackLoader.load(element, new LoadContext(directory, this))
        .mapError(s -> "Failed to load script pack '" + key + "': " + s)
        .resultOrPartial(LOGGER::error);

    if (result.isEmpty()) {
      return;
    }

    PackMeta meta = result.get();
    ScriptPack pack = new ScriptPack(meta, service);

    registerPack(key, pack);
  }

  public void registerPack(String key, ScriptPack pack) {
    var holder = packs.register(key, pack);

    if (started && !activate(holder)) {
      packs.remove(key);
      return;
    }

    LOGGER.debug("Loaded script pack named '{}'", key);
  }

  public void close() {
    for (ScriptPack pack : packs) {
      pack.close();
    }
    packs.clear();
  }
}
