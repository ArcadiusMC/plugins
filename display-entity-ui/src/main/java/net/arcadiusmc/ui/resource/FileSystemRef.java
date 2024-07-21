package net.arcadiusmc.ui.resource;

import java.nio.file.FileSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class FileSystemRef {

  private final FileSystem system;
  final boolean defaultSystem;

  private int referenceCount = 0;

  public void acquire() {
    referenceCount++;
  }

  public void free() {
    referenceCount--;
  }
}
