package net.arcadiusmc.scripts.loader;

import java.nio.file.Path;

public interface ScriptFileListener {

  void onEvent(Path file, String scriptId, FileEvent event);

  enum FileEvent {
    DELETE,
    EDIT,
    CREATE
  }
}
