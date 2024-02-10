package net.arcadiusmc.dialogues;

import java.nio.file.Path;
import lombok.Getter;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;

public class DialogueManager {

  @Getter
  private final Path directory;

  @Getter
  private final Registry<Dialogue> registry = Registries.newRegistry();

  DialogueManager() {
    this.directory = PathUtil.pluginPath();
  }

  public void load() {
    PathUtil.ensureDirectoryExists(directory);
    PluginJar.saveResources("dialogues", directory);

    registry.clear();

    PathUtil.iterateDirectory(directory, true, true, path -> {
      var relative = directory.relativize(path);
      var str = relative.toString().replace(".json", "");

      SerializationHelper.readAsJson(path, json -> {
        var entry = Dialogue.deserialize(JsonWrapper.wrap(json));
        registry.register(str, entry);
      });
    });
  }
}