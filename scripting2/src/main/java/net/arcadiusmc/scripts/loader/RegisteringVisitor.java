package net.arcadiusmc.scripts.loader;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class RegisteringVisitor extends SimpleFileVisitor<Path> {

  static final WatchEvent.Kind[] KINDS = {
      StandardWatchEventKinds.ENTRY_MODIFY,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
  };

  private final List<WatchKey> keys;
  private final WatchService service;

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    WatchKey key = dir.register(service, KINDS);
    keys.add(key);
    return FileVisitResult.CONTINUE;
  }
}
