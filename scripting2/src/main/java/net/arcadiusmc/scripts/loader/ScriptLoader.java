package net.arcadiusmc.scripts.loader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;

public final class ScriptLoader {

  static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Path directory;

  @Getter
  private final Plugin owningPlugin;

  private final Map<String, Source> sourceTable;

  final ScriptFileListener listener;
  WatchService watchService;
  List<WatchKey> watchKeys;
  FileWatchThread watchThread;

  public ScriptLoader(Path directory, Plugin owingPlugin) {
    this(directory, owingPlugin, null);
  }

  public ScriptLoader(Path directory, Plugin owingPlugin, ScriptFileListener listener) {
    Objects.requireNonNull(directory, "Null directory");
    Objects.requireNonNull(owingPlugin, "Null owning plugin");

    this.directory = directory;
    this.owningPlugin = owingPlugin;
    this.listener = listener;
    this.sourceTable = new Object2ObjectOpenHashMap<>();
  }

  public void registerFileWatcher() {
    try {
      if (watchService == null) {
        watchService = directory.getFileSystem().newWatchService();
      }

      if (watchKeys == null) {
        watchKeys = new ArrayList<>();
      }

      RegisteringVisitor visitor = new RegisteringVisitor(watchKeys, watchService);
      Files.walkFileTree(directory, visitor);

      if (watchThread == null) {
        watchThread = new FileWatchThread(this);
        watchThread.start();
      }
    } catch (IOException e) {
      LOGGER.error("Failed to register file watcher for script loader", e);
    }
  }

  public void stopFileWatcher() {
    if (watchService == null || watchKeys == null || watchKeys.isEmpty()) {
      return;
    }

    for (WatchKey watchKey : watchKeys) {
      watchKey.cancel();
    }

    watchKeys.clear();
  }

  public Set<String> getScriptIds() {
    return Collections.unmodifiableSet(sourceTable.keySet());
  }

  public Source get(String scriptId) {
    Objects.requireNonNull(scriptId, "Null script id");
    return sourceTable.get(scriptId);
  }

  void delete(String id) {
    sourceTable.remove(id);
  }

  void put(String id, Source source) {
    sourceTable.put(id, source);
  }
}
