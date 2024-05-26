package net.arcadiusmc.scripts.loader;

import static net.arcadiusmc.scripts.loader.ScriptLoader.LOGGER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicInteger;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.scripts.loader.ScriptFileListener.FileEvent;
import org.graalvm.polyglot.Source;

class FileWatchThread extends Thread {

  private static AtomicInteger idGen = new AtomicInteger();

  private final ScriptLoader loader;
  private final WatchService service;
  private final ScriptFileListener listener;

  public FileWatchThread(ScriptLoader loader) {
    super();

    this.service = loader.watchService;
    this.loader = loader;
    this.listener = loader.listener;

    setName("FileWatchThread-" + idGen.getAndIncrement());
    setUncaughtExceptionHandler(ExceptionHandler.HANDLER);
    setDaemon(true);
  }

  @Override
  public void run() {
    try {
      while (true) {
        unsafeRun();
      }
    } catch (InterruptedException exc) {
      throw new RuntimeException(exc);
    }
  }

  private void unsafeRun() throws InterruptedException {
    WatchKey key = service.take();

    try {
      for (WatchEvent<?> pollEvent : key.pollEvents()) {
        Path path = (Path) pollEvent.context();
        processEvent(path, pollEvent.kind());
      }
    } finally {
      key.reset();
    }
  }

  private void processEvent(Path path, WatchEvent.Kind<?> kind) {
    Path abs = loader.getDirectory().resolve(path);

    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(abs)) {
      try {
        WatchKey key = abs.register(service, RegisteringVisitor.KINDS);
        loader.watchKeys.add(key);
      } catch (IOException e) {
        LOGGER.error("Couldn't register directory watcher for {}", abs, e);
      }

      return;
    }

    String id = path.toString();
    String ext;
    String language;
    int dotIndex = id.lastIndexOf('.');

    if (dotIndex != -1) {
      ext = id.substring(dotIndex);
      id = id.substring(0, dotIndex);

      switch (ext) {
        case ".js", ".mjs", ".cjs" -> {
          language = Scripts.JS_LANGUAGE_ID;
        }

        default -> {
          return;
        }
      }
    } else {
      return;
    }

    FileEvent event;

    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
      loader.delete(id);
      event = FileEvent.DELETE;
    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
      readFile(abs, id, language);
      event = FileEvent.CREATE;
    } else {
      readFile(abs, id, language);
      event = FileEvent.EDIT;
    }

    if (listener != null) {
      listener.onEvent(abs, id, event);
    }
  }

  private void readFile(Path path, String id, String language) {
    String str;

    try {
      str = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException exc) {
      LOGGER.error("Error reading script file {}", path, exc);
      return;
    }

    Source source = Source.newBuilder(language, str, id)
        .buildLiteral();

    loader.put(id, source);
  }
}
