package net.arcadiusmc.dialogues;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Random;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.registry.RegistryListener;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.slf4j.Logger;

public class DialogueManager {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Path directory;

  @Getter
  private final Registry<Dialogue> registry;

  private final LongSet usedIds;
  private final Random random;

  @Getter
  private final Long2ObjectMap<Interaction> interactionIdMap = new Long2ObjectOpenHashMap<>();

  DialogueManager() {
    this.directory = PathUtil.pluginPath();
    this.registry = Registries.newRegistry();

    this.random = new Random();
    this.usedIds = new LongOpenHashSet();

    registry.setListener(new RegistryListener<>() {
      @Override
      public void onRegister(Holder<Dialogue> value) {
        Dialogue dialogue = value.getValue();
        dialogue.setRandomId(genId());

        for (DialogueNode node : dialogue.getNodes()) {
          node.setRandomId(genId());
        }
      }

      @Override
      public void onUnregister(Holder<Dialogue> value) {
        Dialogue dialogue = value.getValue();

        freeId(dialogue.getRandomId());
        dialogue.setRandomId(0);

        for (DialogueNode node : dialogue.getNodes()) {
          freeId(node.getRandomId());
          node.setRandomId(0);
        }
      }
    });
  }

  public long genInteractionId(Interaction interaction) {
    long id = genId();

    interactionIdMap.put(id, interaction);
    interaction.getContext().put("__id", id);

    return id;
  }

  public void freeId(long id) {
    usedIds.remove(id);
  }

  public long genId() {
    long l = random.nextLong();
    int loopCounter = 0;

    while (usedIds.contains(l) && loopCounter < 1000) {
      l = random.nextLong();
      loopCounter++;
    }

    return l;
  }

  public void load() {
    registry.clear();

    PathUtil.ensureDirectoryExists(directory);

    PluginJar.saveResources("example.yml", directory.resolve("example.yml"));
    PluginJar.saveResources("example2.yml", directory.resolve("example2.yml"));

    PathUtil.iterateDirectory(directory, true, true, path -> {
      Path relative = directory.relativize(path);
      String strPath = relative.toString();

      // Skip examples
      if (strPath.contains("example")) {
        return;
      }

      if (!strPath.endsWith(".json")
          && !strPath.endsWith(".yml")
          && !strPath.endsWith(".yaml")
      ) {
        LOGGER.warn("Unsupported dialogue file format found: {}", path);
        return;
      }

      String id = strPath
          .replace(".json", "")
          .replace(".yml", "")
          .replace(".yaml", "");

      SerializationHelper.readAsJson(path, object -> {
        DialogueOptions options;

        if (object.has("settings")) {
          JsonElement element = object.remove("settings");
          options = DialogueOptions.CODEC.parse(JsonOps.INSTANCE, element)
              .mapError(s -> "Failed to read dialogue settings in " + path + ": " + s)
              .resultOrPartial(LOGGER::error)
              .orElse(DialogueOptions.defaultOptions());
        } else {
          options = DialogueOptions.defaultOptions();
        }

        DialogueCodecs.NODEMAP.parse(JsonOps.INSTANCE, object)
            .mapError(s -> "Failed to read dialogues in " + path + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(nodeMap -> {
              Dialogue dialogue = new Dialogue();
              dialogue.setOptions(options);

              nodeMap.forEach((s, node) -> {
                node.getOptions().mergeFrom(options);
                dialogue.addEntry(s, node);
              });

              registry.register(id, dialogue);
              LOGGER.debug("Registered dialogue with ID '{}'", id);
            });
      });
    });
  }
}